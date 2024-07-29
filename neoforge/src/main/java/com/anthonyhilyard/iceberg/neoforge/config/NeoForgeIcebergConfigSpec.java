package com.anthonyhilyard.iceberg.neoforge.config;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.services.IIcebergConfigSpecBuilder;
import com.anthonyhilyard.iceberg.util.UnsafeUtil;
import com.electronwill.nightconfig.core.AbstractCommentedConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import net.neoforged.fml.Logging;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.common.ModConfigSpec.LongValue;
import net.neoforged.neoforge.common.ModConfigSpec.RestartType;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

/*
 * Basically an improved ModConfigSpec that supports subconfigs.
 */
public class NeoForgeIcebergConfigSpec implements IConfigSpec, IIcebergConfigSpec
{
	private final Map<List<String>, String> levelComments;
	private final Map<List<String>, String> levelTranslationKeys;

	private final UnmodifiableConfig spec;
	private final UnmodifiableConfig values;

	@Nullable
	private ILoadedConfig loadedConfig;

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LogManager.getLogger();

	private NeoForgeIcebergConfigSpec(UnmodifiableConfig spec, UnmodifiableConfig values, Map<List<String>, String> levelComments, Map<List<String>, String> levelTranslationKeys)
	{
		this.spec = Config.copy(spec);
		this.values = Config.copy(values);
		this.levelComments = Map.copyOf(levelComments);
		this.levelTranslationKeys = Map.copyOf(levelTranslationKeys);

		// Update the filewatcher's default instance to have a more sensible exception handler.
		try
		{
			Field exceptionHandlerField = FileWatcher.class.getDeclaredField("exceptionHandler");
			UnsafeUtil.setField(exceptionHandlerField, FileWatcher.defaultInstance(), (Consumer<Exception>)e -> LogManager.getLogger().warn(Logging.CORE, "An error occurred while reloading config:", e));
		}
		catch (Exception e) {}
	}

	@Override
	public boolean isEmpty() { return spec.isEmpty(); }

	public String getLevelComment(List<String> path) { return levelComments.get(path); }

	public String getLevelTranslationKey(List<String> path) { return levelTranslationKeys.get(path); }

	@Override
	public void acceptConfig(@Nullable ILoadedConfig config)
	{
		loadedConfig = config;
		if (config != null && !isCorrect(config.config()))
		{
			String configName = config.config() instanceof FileConfig fileConfig ? fileConfig.getNioPath().toString() : config.toString();
			Iceberg.LOGGER.warn("Configuration file {} is not correct. Correcting ", configName);
			correct(config.config(),
					(action, path, incorrectValue, correctedValue) ->
							Iceberg.LOGGER.warn("Incorrect key {} was corrected from {} to its default, {}. {}", DOT_JOINER.join( path ), incorrectValue, correctedValue, incorrectValue == correctedValue ? "This seems to be an error." : ""),
					(action, path, incorrectValue, correctedValue) ->
							Iceberg.LOGGER.debug("The comment on key {} does not match the spec. This may create a backup.", DOT_JOINER.join( path )));

			config.save();
		}
		this.afterReload();
	}

	@Override
	public void validateSpec(ModConfig config)
	{
		// Spec is valid since we only have common config types here.
	}

	@Override
	public boolean isLoaded() { return loadedConfig != null; }

	public UnmodifiableConfig getSpec() { return spec; }

	public UnmodifiableConfig getValues() { return values; }

	private void forEachValue(Iterable<Object> configValues, Consumer<ConfigValue<?>> consumer)
	{
		configValues.forEach(value -> {
			if (value instanceof ConfigValue<?> configValue)
			{
				consumer.accept(configValue);
			}
			else if (value instanceof Config innerConfig)
			{
				forEachValue(innerConfig.valueMap().values(), consumer);
			}
		});
	}

	public void afterReload() { resetCaches(RestartType.NONE); }

	@ApiStatus.Internal
	public void resetCaches(RestartType restartType)
	{
		forEachValue(getValues().valueMap().values(), configValue -> {
			if (configValue.getSpec() == null || configValue.getSpec().restartType() == restartType)
			{
				configValue.clearCache();
			}
		});
	}

	public void save()
	{
		Preconditions.checkNotNull(loadedConfig, "Cannot save config value without assigned Config object present");
		loadedConfig.save();
	}

	@Override
	public boolean isCorrect(UnmodifiableCommentedConfig config)
	{
		LinkedList<String> parentPath = Lists.newLinkedList();
		return correct(spec, config, parentPath, Collections.unmodifiableList(parentPath), (a, b, c, d) -> {}, null, true) == 0;
	}

	@Override
	public void correct(CommentedConfig config)
	{
		correct(config, (action, path, incorrectValue, correctedValue) -> {}, null);
	}

	public int correct(CommentedConfig config, CorrectionListener listener)
	{
		return correct(config, listener, null);
	}

	public int correct(CommentedConfig config, CorrectionListener listener, CorrectionListener commentListener)
	{
		LinkedList<String> parentPath = Lists.newLinkedList();
		return correct(spec, config, parentPath, Collections.unmodifiableList(parentPath), listener, commentListener, false);
	}

	private int correct(UnmodifiableConfig spec, UnmodifiableCommentedConfig config, LinkedList<String> parentPath, List<String> parentPathUnmodifiable, CorrectionListener listener, @Nullable CorrectionListener commentListener, boolean dryRun)
	{
		int count = 0;

		Map<String, Object> specMap = spec.valueMap();
		Map<String, Object> configMap = config.valueMap();

		for (Map.Entry<String, Object> specEntry : specMap.entrySet())
		{
			final String key = specEntry.getKey();
			Object specValue = specEntry.getValue();
			final Object configValue = configMap.get(key);
			final CorrectionAction action = configValue == null ? CorrectionAction.ADD : CorrectionAction.REPLACE;

			parentPath.addLast(key);

			String subConfigComment = null;

			// If this value is a config, use that as the spec value to support subconfigs.
			if (specValue instanceof ValueSpec valueSpec && valueSpec.getDefault() instanceof UnmodifiableConfig)
			{
				subConfigComment = valueSpec.getComment();
				specValue = valueSpec.getDefault();
			}

			if (specValue instanceof Config specConfig)
			{
				if (configValue instanceof Config)
				{
					count += correct(specConfig, configValue instanceof CommentedConfig commentedConfig ? commentedConfig : CommentedConfig.copy((Config)configValue), parentPath, parentPathUnmodifiable, listener, commentListener, dryRun);
					if (count > 0 && dryRun)
					{
						return count;
					}
				}
				else if (dryRun)
				{
					return 1;
				}
				else
				{
					CommentedConfig newValue = ((CommentedConfig)config).createSubConfig();
					configMap.put(key, newValue);
					listener.onCorrect(action, parentPathUnmodifiable, configValue, newValue);
					count++;

					if (specConfig instanceof MutableSubconfig)
					{
						// Fill out subconfig default values.
						specConfig.valueMap().forEach((k, v) -> newValue.valueMap().put(k, v instanceof ValueSpec vSpec ? vSpec.getDefault() : v));
					}
					else
					{
						count += correct((UnmodifiableConfig)specValue, newValue, parentPath, parentPathUnmodifiable, listener, commentListener, dryRun);
					}
				}

				String newComment = subConfigComment == null ? levelComments.get(parentPath) : subConfigComment;
				String oldComment = config.getComment(key);
				if (!stringsMatchNormalizingNewLines(oldComment, newComment))
				{
					if (commentListener != null)
					{
						commentListener.onCorrect(action, parentPathUnmodifiable, oldComment, newComment);
					}

					if (dryRun)
					{
						return 1;
					}

					((CommentedConfig)config).setComment(key, newComment);
				}
			}
			else if (specValue instanceof ValueSpec valueSpec)
			{
				if (!valueSpec.test(configValue))
				{
					if (dryRun)
					{
						return 1;
					}

					Object newValue = valueSpec.correct(configValue);
					configMap.put(key, newValue);
					listener.onCorrect(action, parentPathUnmodifiable, configValue, newValue);
					count++;
				}
				String oldComment = config.getComment(key);
				if (!stringsMatchNormalizingNewLines(oldComment, valueSpec.getComment()))
				{
					if (commentListener != null)
					{
						commentListener.onCorrect(action, parentPathUnmodifiable, oldComment, valueSpec.getComment());
					}

					if (dryRun)
					{
						return 1;
					}

					((CommentedConfig)config).setComment(key, valueSpec.getComment());
				}
			}
			else if (spec instanceof MutableSubconfig subconfig)
			{
				// Check all subconfig entries.
				if (configMap.containsKey(key))
				{
					if (!subconfig.keyValidator().test(key))
					{
						if (dryRun)
						{
							return 1;
						}
						listener.onCorrect(CorrectionAction.REMOVE, parentPathUnmodifiable, key, null);
						configMap.remove(key);
						count++;
					}

					if (!subconfig.valueValidator().test(configMap.get(key)))
					{
						if (dryRun)
						{
							return 1;
						}
						listener.onCorrect(CorrectionAction.REMOVE, parentPathUnmodifiable, configMap.get(key), null);
						configMap.remove(key);
						count++;
					}
				}
			}

			parentPath.removeLast();
		}

		// Now remove any config values that are not explicitly set in the spec.
		for (Iterator<Map.Entry<String, Object>> iterator = configMap.entrySet().iterator(); iterator.hasNext();)
		{
			Map.Entry<String, Object> entry = iterator.next();

			// If the spec is a dynamic subconfig, don't bother checking the spec since that's the point.
			if (!(spec instanceof MutableSubconfig) && !specMap.containsKey(entry.getKey()))
			{
				if (dryRun)
				{
					return 1;
				}

				iterator.remove();
				parentPath.addLast(entry.getKey());
				listener.onCorrect(CorrectionAction.REMOVE, parentPathUnmodifiable, entry.getValue(), null);
				parentPath.removeLast();
				count++;
			}
		}
		return count;
	}

	private boolean stringsMatchNormalizingNewLines(@Nullable String string1, @Nullable String string2)
	{
		boolean blank1 = string1 == null || string1.isBlank();
		boolean blank2 = string2 == null || string2.isBlank();
		if (blank1 != blank2)
		{
			return false;
		}
		else if (blank1 && blank2)
		{
			return true;
		}
		else
		{
			return string1.replaceAll("\r\n", "\n").equals(string2.replaceAll("\r\n", "\n"));
		}
	}

	public static ValueSpec createValueSpec(String comment, String langKey, boolean worldRestart, Class<?> clazz, Supplier<?> defaultSupplier, Predicate<Object> validator, RestartType restartType)
	{
		Objects.requireNonNull(defaultSupplier, "Default supplier can not be null!");
		Objects.requireNonNull(validator, "Validator can not be null!");

		// Instantiate the new ValueSpec instance, then use reflection to set the required fields.
		ValueSpec result = UnsafeUtil.newInstance(ValueSpec.class);
		try
		{
			Field commentField = ValueSpec.class.getDeclaredField("comment");
			Field langKeyField = ValueSpec.class.getDeclaredField("langKey");
			Field rangeField = ValueSpec.class.getDeclaredField("range");
			Field worldRestartField = ValueSpec.class.getDeclaredField("worldRestart");
			Field clazzField = ValueSpec.class.getDeclaredField("clazz");
			Field supplierField = ValueSpec.class.getDeclaredField("supplier");
			Field validatorField = ValueSpec.class.getDeclaredField("validator");
			Field restartTypeField = ValueSpec.class.getDeclaredField("restartType");

			UnsafeUtil.setField(commentField, result, comment);
			UnsafeUtil.setField(langKeyField, result, langKey);
			UnsafeUtil.setField(rangeField, result, null);
			UnsafeUtil.setField(worldRestartField, result, worldRestart);
			UnsafeUtil.setField(clazzField, result, clazz);
			UnsafeUtil.setField(supplierField, result, defaultSupplier);
			UnsafeUtil.setField(validatorField, result, validator);
			UnsafeUtil.setField(restartTypeField, result, restartType);
		}
		catch (Exception e)
		{
			Iceberg.LOGGER.warn("Failed to instantiate ValueSpec!");
			Iceberg.LOGGER.warn(ExceptionUtils.getStackTrace(e));
		}

		return result;
	}

	public ILoadedConfig loadedConfig()
	{
		return loadedConfig;
	}

	public static class Builder extends ModConfigSpec.Builder implements IIcebergConfigSpecBuilder
	{
		@Override
		public Builder comment(String comment)
		{
			return (Builder) super.comment(comment);
		}

		@Override
		public Builder comment(String... comment)
		{
			return (Builder) super.comment(comment);
		}

		@Override
		public Builder translation(String translationKey)
		{
			return (Builder) super.translation(translationKey);
		}

		@Override
		public Builder worldRestart()
		{
			return (Builder) super.worldRestart();
		}

		@Override
		public Builder push(String path)
		{
			return (Builder) super.push(path);
		}

		@Override
		public Builder push(List<String> path)
		{
			return (Builder) super.push(path);
		}

		@Override
		public Builder pop()
		{
			return (Builder) super.pop();
		}

		@Override
		public Builder pop(int count)
		{
			return (Builder) super.pop(count);
		}

		private NeoForgeIcebergConfigSpec finishBuild()
		{
			NeoForgeIcebergConfigSpec result = null;

			try
			{
				Field valuesField = ModConfigSpec.Builder.class.getDeclaredField("values");
				Field storageField = ModConfigSpec.Builder.class.getDeclaredField("spec");
				Field levelCommentsField = ModConfigSpec.Builder.class.getDeclaredField("levelComments");
				Field levelTranslationKeysField = ModConfigSpec.Builder.class.getDeclaredField("levelTranslationKeys");

				List<ConfigValue<?>> values = UnsafeUtil.getField(valuesField, this);

				Config storage = UnsafeUtil.getField(storageField, this);
				Map<List<String>, String> levelComments = UnsafeUtil.getField(levelCommentsField, this);
				Map<List<String>, String> levelTranslationKeys = UnsafeUtil.getField(levelTranslationKeysField, this);

				@SuppressWarnings("deprecation")
				Config valueCfg = Config.of(Config.getDefaultMapCreator(true, true), InMemoryFormat.withSupport(ConfigValue.class::isAssignableFrom));
				values.forEach(v -> valueCfg.set(v.getPath(), v));

				final NeoForgeIcebergConfigSpec ret = new NeoForgeIcebergConfigSpec(storage.unmodifiable(), valueCfg.unmodifiable(), Collections.unmodifiableMap(levelComments), Collections.unmodifiableMap(levelTranslationKeys));

				Field specField = ConfigValue.class.getDeclaredField("spec");
				values.forEach(v -> UnsafeUtil.setField(specField, v, ret));
				result = ret;
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.warn("Failed to build NeoForgeIcebergConfigSpec!");
				Iceberg.LOGGER.warn(ExceptionUtils.getStackTrace(e));
			}

			return result;
		}

		
		@Override
		public void reset()
		{
			try
			{
				Field valuesField = ModConfigSpec.Builder.class.getDeclaredField("values");
				Field storageField = ModConfigSpec.Builder.class.getDeclaredField("spec");
				Field levelCommentsField = ModConfigSpec.Builder.class.getDeclaredField("levelComments");
				Field levelTranslationKeysField = ModConfigSpec.Builder.class.getDeclaredField("levelTranslationKeys");

				List<ConfigValue<?>> values = UnsafeUtil.getField(valuesField, this);
				Config storage = UnsafeUtil.getField(storageField, this);
				Map<List<String>, String> levelComments = UnsafeUtil.getField(levelCommentsField, this);
				Map<List<String>, String> levelTranslationKeys = UnsafeUtil.getField(levelTranslationKeysField, this);

				storage.clear();
				levelComments.clear();
				levelTranslationKeys.clear();
				values.clear();
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.warn(ExceptionUtils.getStackTrace(e));
			}
		}

		@Override
		public <T> Pair<T, IIcebergConfigSpec> finish(Function<IIcebergConfigSpecBuilder, T> consumer)
		{
			T o = consumer.apply(this);
			return Pair.of(o, this.finishBuild());
		}

		private <T, S extends ConfigValue<T>> ConfigValueWrapper<T, S> wrap(S value)
		{
			return new ConfigValueWrapper<>(value);
		}

		@Override
		public <T> Supplier<T> add(String path, T defaultValue)
		{
			ConfigValue<T> value = define(path, defaultValue);
			return () -> wrap(value).get();
		}

		@Override
		public <T> Supplier<T> add(String path, T defaultValue, Predicate<Object> validator)
		{
			ConfigValue<T> value = define(path, defaultValue, validator);
			return () -> wrap(value).get();
		}

		@Override
		public <V extends Comparable<? super V>> Supplier<V> addInRange(String path, V defaultValue, V min, V max, Class<V> clazz)
		{
			ConfigValue<V> value = defineInRange(path, defaultValue, min, max, clazz);
			return () -> wrap(value).get();
		}

		@Override
		public <T> Supplier<T> addInList(String path, T defaultValue, Collection<? extends T> acceptableValues)
		{
			ConfigValue<T> value = defineInList(path, defaultValue, acceptableValues);
			return () -> wrap(value).get();
		}

		@Override
		@SuppressWarnings("deprecation")
		public <T> Supplier<List<? extends T>> addList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator)
		{
			ConfigValue<List<? extends T>> value = defineList(path, defaultValue, elementValidator);
			return () -> wrap(value).get();
		}

		@Override
		@SuppressWarnings("deprecation")
		public <T> Supplier<List<? extends T>> addListAllowEmpty(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator)
		{
			ConfigValue<List<? extends T>> value = defineListAllowEmpty(path, defaultValue, elementValidator);
			return () -> wrap(value).get();
		}

		@Override
		public <V extends Enum<V>> Supplier<V> addEnum(String path, V defaultValue)
		{
			EnumValue<V> value = defineEnum(path, defaultValue);
			return () -> wrap(value).get();
		}

		@Override
		public <V extends Enum<V>> Supplier<V> addEnum(String path, V defaultValue, Predicate<Object> validator)
		{
			EnumValue<V> value = defineEnum(path, defaultValue, validator);
			return () -> wrap(value).get();
		}

		@Override
		public Supplier<Boolean> add(String path, boolean defaultValue)
		{
			BooleanValue value = define(path, defaultValue);
			return () -> wrap(value).get();
		}

		@Override
		public Supplier<Double> addInRange(String path, double defaultValue, double min, double max)
		{
			DoubleValue value = defineInRange(path, defaultValue, min, max);
			return () -> wrap(value).get();
		}

		@Override
		public Supplier<Integer> addInRange(String path, int defaultValue, int min, int max)
		{
			IntValue value = defineInRange(path, defaultValue, min, max);
			return () -> wrap(value).get();
		}

		@Override
		public Supplier<Long> addInRange(String path, long defaultValue, long min, long max)
		{
			LongValue value = defineInRange(path, defaultValue, min, max);
			return () -> wrap(value).get();
		}

		@Override
		public Supplier<Map<String, Object>> addSubconfig(String path, Map<String, Object> defaultValue, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			return addSubconfig(split(path), defaultValue, keyValidator, valueValidator);
		}

		public Supplier<Map<String, Object>> addSubconfig(List<String> path, Map<String, Object> defaultValue, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			return addSubconfig(path, () -> defaultValue, keyValidator, valueValidator);
		}

		public Supplier<Map<String, Object>> addSubconfig(String path, Supplier<Map<String, Object>> defaultSupplier, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			return addSubconfig(split(path), defaultSupplier, keyValidator, valueValidator);
		}

		public Supplier<Map<String, Object>> addSubconfig(List<String> path, Supplier<Map<String, Object>> defaultSupplier, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			final UnmodifiableConfig defaultConfig = Config.of(defaultSupplier, TomlFormat.instance());
			ConfigValue<Config> value = define(path, () -> MutableSubconfig.copy(defaultConfig, keyValidator, valueValidator), o -> o != null);
			return () -> wrap(value).get().valueMap();
		}
	}

	/**
	 * This class is specifically meant for dynamic subconfigs--subconfigs where both keys and values are mutable.
	 */
	final public static class MutableSubconfig extends AbstractCommentedConfig
	{
		private final ConfigFormat<?> configFormat;
		private final Predicate<Object> keyValidator;
		private final Predicate<Object> valueValidator;
		private static ValueSpec defaultValueSpec = null;

		/**
		 * Creates a Subconfig by copying a config and with the specified format.
		 *
		 * @param toCopy       the config to copy
		 * @param configFormat the config's format
		 */
		@SuppressWarnings("deprecation")
		MutableSubconfig(UnmodifiableConfig toCopy, ConfigFormat<?> configFormat, boolean concurrent, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			super(toCopy, concurrent);
			this.configFormat = configFormat;
			this.keyValidator = keyValidator;
			this.valueValidator = valueValidator;
		}

		// Returns a value spec to use for each entry in this subconfig.
		public ValueSpec defaultValueSpec()
		{
			if (defaultValueSpec == null)
			{
				defaultValueSpec = createValueSpec(null, null, false, Object.class, () -> null, valueValidator, RestartType.NONE);
			}
			return defaultValueSpec;
		}

		@Override
		public ConfigFormat<?> configFormat()
		{
			return configFormat;
		}

		public Predicate<Object> keyValidator()
		{
			return keyValidator;
		}

		public Predicate<Object> valueValidator()
		{
			return valueValidator;
		}

		/**
		 * Creates a new Subconfig with the content of the given config. The returned config will have
		 * the same format as the copied config.
		 *
		 * @param config the config to copy
		 * @return a copy of the config
		 */
		public static MutableSubconfig copy(UnmodifiableConfig config, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			return new MutableSubconfig(config, config.configFormat(), false, keyValidator, valueValidator);
		}

		@Override
		public CommentedConfig createSubConfig() { throw new UnsupportedOperationException("Can't make a subconfig of a mutable subconfig!"); }

		@Override
		public AbstractCommentedConfig clone() { throw new UnsupportedOperationException("Can't clone a mutable subconfig!"); }
	}

	@SuppressWarnings("unused")
	private static final Joiner LINE_JOINER = Joiner.on("\n");
	private static final Joiner DOT_JOINER = Joiner.on(".");
	private static final Splitter DOT_SPLITTER = Splitter.on(".");
	private static List<String> split(String path)
	{
		return Lists.newArrayList(DOT_SPLITTER.split(path));
	}
}