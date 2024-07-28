package com.anthonyhilyard.iceberg.forge.config;

import java.io.File;
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
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.LongValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;
import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.config.IConfigSpec;

/*
 * Basically an improved ModConfigSpec that supports subconfigs.
 */
public class ForgeIcebergConfigSpec extends UnmodifiableConfigWrapper<UnmodifiableConfig> implements IConfigSpec<ForgeIcebergConfigSpec>, IIcebergConfigSpec
{
	private Map<List<String>, String> levelComments;
	private Map<List<String>, String> levelTranslationKeys;

	private UnmodifiableConfig values;
	private Config childConfig;

	private boolean isCorrecting = false;

	private ForgeIcebergConfigSpec(UnmodifiableConfig storage, UnmodifiableConfig values, Map<List<String>, String> levelComments, Map<List<String>, String> levelTranslationKeys)
	{
		super(Config.copy(storage));
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

	public String getLevelComment(List<String> path)
	{
		return levelComments.get(path);
	}

	public String getLevelTranslationKey(List<String> path)
	{
		return levelTranslationKeys.get(path);
	}

	public void setConfig(CommentedConfig config)
	{
		this.childConfig = config;
		if (config != null && !isCorrect(config))
		{
			String configName = config instanceof FileConfig fileConfig ? fileConfig.getNioPath().toString() : config.toString();
			Iceberg.LOGGER.warn("Configuration file {} is not correct. Correcting", configName);
			correct(config,
					(action, path, incorrectValue, correctedValue) ->
							Iceberg.LOGGER.warn("Incorrect key {} was corrected from {} to its default, {}. {}", DOT_JOINER.join( path ), incorrectValue, correctedValue, incorrectValue == correctedValue ? "This seems to be an error." : ""),
					(action, path, incorrectValue, correctedValue) ->
							Iceberg.LOGGER.debug("The comment on key {} does not match the spec. This may create a backup.", DOT_JOINER.join( path )));

			if (config instanceof FileConfig fileConfig)
			{
				fileConfig.save();
			}
		}
		this.afterReload();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getRaw(List<String> path)
	{
		T value = super.getRaw(path);
		if (value != null)
		{
			return value;
		}
		// Try to "recursively" get the value if needed.
		List<String> subPath = path.subList(0, path.size() - 1);
		Object test = super.getRaw(subPath);
		if (test instanceof ValueSpec valueSpec && valueSpec.getDefault() instanceof MutableSubconfig subconfig)
		{
			// Okay, this is a dynamic subconfig.  That means that only values defined in the default have
			// an actual value spec.
			value = subconfig.getRaw(path.get(path.size() - 1));

			// Value will be null here for non-default entries.  In that case, just return a default value spec.
			if (value == null)
			{
				value = (T) subconfig.defaultValueSpec();
			}
			return value;
		}
		return null;
	}

	@Override
	public void acceptConfig(final CommentedConfig data)
	{
		setConfig(data);
	}

	public boolean isCorrecting()
	{
		return isCorrecting;
	}

	public boolean isLoaded()
	{
		return childConfig != null;
	}

	public UnmodifiableConfig getSpec()
	{
		return this.config;
	}

	public UnmodifiableConfig getValues()
	{
		return this.values;
	}

	public void afterReload()
	{
		this.resetCaches(getValues().valueMap().values());
	}

	private void resetCaches(final Iterable<Object> configValues)
	{
		configValues.forEach(value ->
		{
			if (value instanceof ConfigValue<?> configValue)
			{
				configValue.clearCache();
			}
			else if (value instanceof Config innerConfig)
			{
				this.resetCaches(innerConfig.valueMap().values());
			}
		});
	}

	public void save()
	{
		Preconditions.checkNotNull(childConfig, "Cannot save config value without assigned Config object present!");
		if (childConfig instanceof FileConfig fileConfig)
		{
			fileConfig.save();
		}
	}

	public synchronized boolean isCorrect(CommentedConfig config)
	{
		LinkedList<String> parentPath = new LinkedList<>();

		// Forge's config watcher isn't properly atomic, so sometimes this method can give false negatives leading
		// to the entire config file reverting to defaults.  To prevent this, we'll check for an invalid state and
		// skip the correction process when that happens.
		if (config.valueMap().isEmpty() && config instanceof FileConfig fileConfig)
		{
			File configFile = fileConfig.getFile();

			// Sleep for 10 ms to give it a chance to catch up.  This shouldn't cause any issues since
			// this method only runs when the file changes and at startup.
			try { Thread.sleep(10); } catch (Exception e) { }

			// The file isn't actually empty, so this is an invalid state.  Skip this correction phase.
			if (configFile.length() > 0)
			{
				return true;
			}
		}

		return correct(this.config, config, parentPath, Collections.unmodifiableList( parentPath ), (a, b, c, d) -> {}, null, true) == 0;
	}

	public synchronized int correct(CommentedConfig config)
	{
		return correct(config, (action, path, incorrectValue, correctedValue) -> {}, null);
	}

	public synchronized int correct(CommentedConfig config, CorrectionListener listener)
	{
		return correct(config, listener, null);
	}

	public synchronized int correct(CommentedConfig config, CorrectionListener listener, CorrectionListener commentListener)
	{
		LinkedList<String> parentPath = new LinkedList<>();
		int ret = -1;
		try
		{
			isCorrecting = true;
			ret = correct(this.config, config, parentPath, Collections.unmodifiableList(parentPath), listener, commentListener, false);
		}
		finally
		{
			isCorrecting = false;
		}
		return ret;
	}

	private synchronized int correct(UnmodifiableConfig spec, CommentedConfig config, LinkedList<String> parentPath, List<String> parentPathUnmodifiable, CorrectionListener listener, CorrectionListener commentListener, boolean dryRun)
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

			if (specValue instanceof UnmodifiableConfig specConfig)
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
					CommentedConfig newValue = config.createSubConfig();
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
				if (!stringsMatchIgnoringNewlines(oldComment, newComment))
				{
					if (commentListener != null)
					{
						commentListener.onCorrect(action, parentPathUnmodifiable, oldComment, newComment);
					}

					if (dryRun)
					{
						return 1;
					}

					config.setComment(key, newComment);
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
				if (!stringsMatchIgnoringNewlines(oldComment, valueSpec.getComment()))
				{
					if (commentListener != null)
					{
						commentListener.onCorrect(action, parentPathUnmodifiable, oldComment, valueSpec.getComment());
					}

					if (dryRun)
					{
						return 1;
					}

					config.setComment(key, valueSpec.getComment());
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

	private boolean stringsMatchIgnoringNewlines(@Nullable Object obj1, @Nullable Object obj2)
	{
		if (obj1 instanceof String && obj2 instanceof String)
		{
			String string1 = (String) obj1;
			String string2 = (String) obj2;

			if (string1.length() > 0 && string2.length() > 0)
			{
				return string1.replaceAll("\r\n", "\n").equals(string2.replaceAll("\r\n", "\n"));
			}
		}

		// Fallback for when we're not given Strings, or one of them is empty
		return Objects.equals(obj1, obj2);
	}

	public static ValueSpec createValueSpec(String comment, String langKey, boolean worldRestart, Class<?> clazz, Supplier<?> defaultSupplier, Predicate<Object> validator)
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
			UnsafeUtil.setField(commentField, result, comment);
			UnsafeUtil.setField(langKeyField, result, langKey);
			UnsafeUtil.setField(rangeField, result, null);
			UnsafeUtil.setField(worldRestartField, result, worldRestart);
			UnsafeUtil.setField(clazzField, result, clazz);
			UnsafeUtil.setField(supplierField, result, defaultSupplier);
			UnsafeUtil.setField(validatorField, result, validator);
		}
		catch (Exception e)
		{
			Iceberg.LOGGER.warn("Failed to instantiate ValueSpec!");
			Iceberg.LOGGER.warn(ExceptionUtils.getStackTrace(e));
		}

		return result;
	}

	public static class Builder extends ForgeConfigSpec.Builder implements IIcebergConfigSpecBuilder
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

		@SuppressWarnings("deprecation")
		private ForgeIcebergConfigSpec finishBuild()
		{
			ForgeIcebergConfigSpec result = null;

			try
			{
				Field valuesField = ForgeConfigSpec.Builder.class.getDeclaredField("values");
				Field storageField = ForgeConfigSpec.Builder.class.getDeclaredField("storage");
				Field levelCommentsField = ForgeConfigSpec.Builder.class.getDeclaredField("levelComments");
				Field levelTranslationKeysField = ForgeConfigSpec.Builder.class.getDeclaredField("levelTranslationKeys");

				List<ConfigValue<?>> values = UnsafeUtil.getField(valuesField, this);

				Config storage = UnsafeUtil.getField(storageField, this);
				Map<List<String>, String> levelComments = UnsafeUtil.getField(levelCommentsField, this);
				Map<List<String>, String> levelTranslationKeys = UnsafeUtil.getField(levelTranslationKeysField, this);

				Config valueCfg = Config.of(Config.getDefaultMapCreator(true, true), InMemoryFormat.withSupport(ConfigValue.class::isAssignableFrom));
				values.forEach(v -> valueCfg.set(v.getPath(), v));

				final ForgeIcebergConfigSpec ret = new ForgeIcebergConfigSpec(storage, valueCfg, levelComments, levelTranslationKeys);

				Field specField = ConfigValue.class.getDeclaredField("spec");
				values.forEach(v -> {
					try
					{
						UnsafeUtil.setField(specField, v, ret);
					}
					catch (Exception e)
					{
						Iceberg.LOGGER.warn("Failed to create spec field {}!", v.toString());
						Iceberg.LOGGER.warn(ExceptionUtils.getStackTrace(e));
					}
				});
				result = ret;
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.warn("Failed to build IcebergConfigSpec!");
				Iceberg.LOGGER.warn(ExceptionUtils.getStackTrace(e));
			}

			return result;
		}

		@Override
		public void reset()
		{
			try
			{
				Field valuesField = ForgeConfigSpec.Builder.class.getDeclaredField("values");
				Field storageField = ForgeConfigSpec.Builder.class.getDeclaredField("storage");
				Field levelCommentsField = ForgeConfigSpec.Builder.class.getDeclaredField("levelComments");
				Field levelTranslationKeysField = ForgeConfigSpec.Builder.class.getDeclaredField("levelTranslationKeys");

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

		@Override
		public <T> Supplier<T> add(String path, T defaultValue)
		{
			ConfigValue<T> value = define(path, defaultValue);
			return () -> value.get();
		}

		@Override
		public <T> Supplier<T> add(String path, T defaultValue, Predicate<Object> validator)
		{
			ConfigValue<T> value = define(path, defaultValue, validator);
			return () -> value.get();
		}

		@Override
		public <V extends Comparable<? super V>> Supplier<V> addInRange(String path, V defaultValue, V min, V max, Class<V> clazz)
		{
			ConfigValue<V> value = defineInRange(path, defaultValue, min, max, clazz);
			return () -> value.get();
		}

		@Override
		public <T> Supplier<T> addInList(String path, T defaultValue, Collection<? extends T> acceptableValues)
		{
			ConfigValue<T> value = defineInList(path, defaultValue, acceptableValues);
			return () -> value.get();
		}

		@Override
		public <T> Supplier<List<? extends T>> addList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator)
		{
			ConfigValue<List<? extends T>> value = defineList(path, defaultValue, elementValidator);
			return () -> value.get();
		}

		@Override
		public <T> Supplier<List<? extends T>> addListAllowEmpty(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator)
		{
			ConfigValue<List<? extends T>> value = defineListAllowEmpty(path, defaultValue, elementValidator);
			return () -> value.get();
		}

		@Override
		public <V extends Enum<V>> Supplier<V> addEnum(String path, V defaultValue)
		{
			EnumValue<V> value = defineEnum(path, defaultValue);
			return () -> value.get();
		}

		@Override
		public <V extends Enum<V>> Supplier<V> addEnum(String path, V defaultValue, Predicate<Object> validator)
		{
			EnumValue<V> value = defineEnum(path, defaultValue, validator);
			return () -> value.get();
		}

		@Override
		public Supplier<Boolean> add(String path, boolean defaultValue)
		{
			BooleanValue value = define(path, defaultValue);
			return () -> value.get();
		}

		@Override
		public Supplier<Double> addInRange(String path, double defaultValue, double min, double max)
		{
			DoubleValue value = defineInRange(path, defaultValue, min, max);
			return () -> value.get();
		}

		@Override
		public Supplier<Integer> addInRange(String path, int defaultValue, int min, int max)
		{
			IntValue value = defineInRange(path, defaultValue, min, max);
			return () -> value.get();
		}

		@Override
		public Supplier<Long> addInRange(String path, long defaultValue, long min, long max)
		{
			LongValue value = defineInRange(path, defaultValue, min, max);
			return () -> value.get();
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
			return () -> value.get().valueMap();
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
				defaultValueSpec = createValueSpec(null, null, false, Object.class, () -> null, valueValidator);
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

	private static final Joiner DOT_JOINER = Joiner.on(".");
	private static final Splitter DOT_SPLITTER = Splitter.on(".");
	private static List<String> split(String path)
	{
		return Lists.newArrayList(DOT_SPLITTER.split(path));
	}
}