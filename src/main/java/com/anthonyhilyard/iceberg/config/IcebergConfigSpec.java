package com.anthonyhilyard.iceberg.config;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;
import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.unsafe.UnsafeHacks;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

import com.anthonyhilyard.iceberg.Loader;
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
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/*
 * Basically an improved ForgeConfigSpec that supports subconfigs.
 */
public class IcebergConfigSpec extends UnmodifiableConfigWrapper<UnmodifiableConfig> implements IConfigSpec<IcebergConfigSpec>
{
	private Map<List<String>, String> levelComments;
	private Map<List<String>, String> levelTranslationKeys;

	private UnmodifiableConfig values;
	private Config childConfig;

	private boolean isCorrecting = false;

	private IcebergConfigSpec(UnmodifiableConfig storage, UnmodifiableConfig values, Map<List<String>, String> levelComments, Map<List<String>, String> levelTranslationKeys)
	{
		super(storage);
		this.values = values;
		this.levelComments = levelComments;
		this.levelTranslationKeys = levelTranslationKeys;

		// Update the filewatcher's default instance to have a more sensible exception handler.
		try
		{
			Field exceptionHandlerField = FileWatcher.class.getDeclaredField("exceptionHandler");
			UnsafeHacks.setField(exceptionHandlerField, FileWatcher.defaultInstance(), (Consumer<Exception>)e -> {
				LogManager.getLogger().warn(Logging.CORE, "An error occurred while reloading config:", e);
			});
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
			Loader.LOGGER.warn("Configuration file {} is not correct. Correcting", configName);
			correct(config,
					(action, path, incorrectValue, correctedValue) ->
							Loader.LOGGER.warn("Incorrect key {} was corrected from {} to its default, {}. {}", DOT_JOINER.join( path ), incorrectValue, correctedValue, incorrectValue == correctedValue ? "This seems to be an error." : ""),
					(action, path, incorrectValue, correctedValue) ->
							Loader.LOGGER.debug("The comment on key {} does not match the spec. This may create a backup.", DOT_JOINER.join( path )));

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

			if (specValue instanceof UnmodifiableConfig)
			{
				if (configValue instanceof Config)
				{
					count += correct((UnmodifiableConfig)specValue, configValue instanceof CommentedConfig commentedConfig ? commentedConfig : CommentedConfig.copy((Config)configValue), parentPath, parentPathUnmodifiable, listener, commentListener, dryRun);
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
					count += correct((UnmodifiableConfig)specValue, newValue, parentPath, parentPathUnmodifiable, listener, commentListener, dryRun);
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
			else
			{
				ValueSpec valueSpec = (ValueSpec)specValue;
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
		ValueSpec result = UnsafeHacks.newInstance(ValueSpec.class);
		try
		{
			Field commentField = ValueSpec.class.getDeclaredField("comment");
			Field langKeyField = ValueSpec.class.getDeclaredField("langKey");
			Field rangeField = ValueSpec.class.getDeclaredField("range");
			Field worldRestartField = ValueSpec.class.getDeclaredField("worldRestart");
			Field clazzField = ValueSpec.class.getDeclaredField("clazz");
			Field supplierField = ValueSpec.class.getDeclaredField("supplier");
			Field validatorField = ValueSpec.class.getDeclaredField("validator");
			UnsafeHacks.setField(commentField, result, comment);
			UnsafeHacks.setField(langKeyField, result, langKey);
			UnsafeHacks.setField(rangeField, result, null);
			UnsafeHacks.setField(worldRestartField, result, worldRestart);
			UnsafeHacks.setField(clazzField, result, clazz);
			UnsafeHacks.setField(supplierField, result, defaultSupplier);
			UnsafeHacks.setField(validatorField, result, validator);
		}
		catch (Exception e) {
			Loader.LOGGER.warn("Failed to instantiate ValueSpec!");
			Loader.LOGGER.warn(ExceptionUtils.getStackTrace(e));
		 }

		return result;
	}

	public static class Builder extends ForgeConfigSpec.Builder
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

		public ConfigValue<UnmodifiableConfig> defineSubconfig(String path, UnmodifiableConfig defaultValue, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			return defineSubconfig(split(path), defaultValue, keyValidator, valueValidator);
		}

		public ConfigValue<UnmodifiableConfig> defineSubconfig(List<String> path, UnmodifiableConfig defaultValue, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			return defineSubconfig(path, () -> defaultValue, keyValidator, valueValidator);
		}

		public ConfigValue<UnmodifiableConfig> defineSubconfig(String path, Supplier<UnmodifiableConfig> defaultSupplier, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			return defineSubconfig(split(path), defaultSupplier, keyValidator, valueValidator);
		}

		public ConfigValue<UnmodifiableConfig> defineSubconfig(List<String> path, Supplier<UnmodifiableConfig> defaultSupplier, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			final UnmodifiableConfig defaultConfig = defaultSupplier.get();
			return define(path, () -> MutableSubconfig.copy(defaultConfig, keyValidator, valueValidator), o -> o != null);
		}

		private IcebergConfigSpec finishBuild()
		{
			IcebergConfigSpec result = null;

			try
			{
				Field valuesField = ForgeConfigSpec.Builder.class.getDeclaredField("values");
				Field storageField = ForgeConfigSpec.Builder.class.getDeclaredField("storage");
				Field levelCommentsField = ForgeConfigSpec.Builder.class.getDeclaredField("levelComments");
				Field levelTranslationKeysField = ForgeConfigSpec.Builder.class.getDeclaredField("levelTranslationKeys");

				List<ConfigValue<?>> values = UnsafeHacks.<List<ConfigValue<?>>>getField(valuesField, this);
				Config storage = UnsafeHacks.<Config>getField(storageField, this);
				Map<List<String>, String> levelComments = UnsafeHacks.<Map<List<String>, String>>getField(levelCommentsField, this);
				Map<List<String>, String> levelTranslationKeys = UnsafeHacks.<Map<List<String>, String>>getField(levelTranslationKeysField, this);

				Config valueCfg = Config.of(Config.getDefaultMapCreator(true, true), InMemoryFormat.withSupport(ConfigValue.class::isAssignableFrom));
				values.forEach(v -> valueCfg.set(v.getPath(), v));

				final IcebergConfigSpec ret = new IcebergConfigSpec(storage, valueCfg, levelComments, levelTranslationKeys);
				values.forEach(v -> {
					try
					{
						Field specField = ConfigValue.class.getDeclaredField("spec");
						UnsafeHacks.setField(specField, v, ret);
					}
					catch (Exception e) {
						Loader.LOGGER.warn("Failed to create spec field {}!", v.toString());
						Loader.LOGGER.warn(ExceptionUtils.getStackTrace(e));
					 }
				});
				result = ret;
			}
			catch (Exception e) {
				Loader.LOGGER.warn("Failed to build IcebergConfigSpec!");
				Loader.LOGGER.warn(ExceptionUtils.getStackTrace(e));
			 }
			return result;
		}

		<T> Pair<T, IcebergConfigSpec> finish(Function<IcebergConfigSpec.Builder, T> consumer)
		{
			T o = consumer.apply(this);
			return Pair.of(o, this.finishBuild());
		}

		@Deprecated
		@Override
		public <T> Pair<T, ForgeConfigSpec> configure(Function<ForgeConfigSpec.Builder, T> consumer)
		{
			throw new UnsupportedOperationException("Configure method not supported.  Use IcebergConfig instead.");
		}

		@Deprecated
		@Override
		public ForgeConfigSpec build()
		{
			throw new UnsupportedOperationException("Build method not supported.  Use IcebergConfig instead.");
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
		public CommentedConfig createSubConfig() { throw new UnsupportedOperationException("Can't make a subconfig of a dynamic subconfig!"); }

		@Override
		public AbstractCommentedConfig clone() { throw new UnsupportedOperationException("Can't clone a dynamic subconfig!"); }
	}

	private static final Joiner DOT_JOINER = Joiner.on(".");
	private static final Splitter DOT_SPLITTER = Splitter.on(".");
	private static List<String> split(String path)
	{
		return Lists.newArrayList(DOT_SPLITTER.split(path));
	}
}
