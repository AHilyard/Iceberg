package com.anthonyhilyard.iceberg.fabric.config;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.BiConsumer;
import org.jetbrains.annotations.Nullable;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;
import com.anthonyhilyard.iceberg.services.IIcebergConfigSpecBuilder;
import com.anthonyhilyard.iceberg.util.UnsafeUtil;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.utils.StringUtils;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@SuppressWarnings("unused")
public class FabricIcebergConfigSpec extends UnmodifiableConfigWrapper<UnmodifiableConfig> implements IIcebergConfigSpec
{
	private Map<List<String>, String> levelComments;
	private Map<List<String>, String> levelTranslationKeys;

	private UnmodifiableConfig values;
	private Config childConfig;

	private boolean isCorrecting = false;

	private FabricIcebergConfigSpec(UnmodifiableConfig storage, UnmodifiableConfig values, Map<List<String>, String> levelComments, Map<List<String>, String> levelTranslationKeys)
	{
		super(Config.copy(storage));
		this.values = Config.copy(values);
		this.levelComments = Map.copyOf(levelComments);
		this.levelTranslationKeys = Map.copyOf(levelTranslationKeys);

		// Update the filewatcher's default instance to have a more sensible exception handler.
		try
		{
			Field exceptionHandlerField = FileWatcher.class.getDeclaredField("exceptionHandler");
			UnsafeUtil.setField(exceptionHandlerField, FileWatcher.defaultInstance(), (Consumer<Exception>)e -> Iceberg.LOGGER.warn("An error occurred while reloading config:", e));
		}
		catch (Exception e) {}
	}

	public void save()
	{
		if (childConfig instanceof FileConfig fileConfig)
		{
			fileConfig.save();
		}
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
							Iceberg.LOGGER.warn("Incorrect key {} was corrected from {} to its default, {}. {}", String.join(".", path), incorrectValue, correctedValue, incorrectValue == correctedValue ? "This seems to be an error." : ""),
					(action, path, incorrectValue, correctedValue) ->
							Iceberg.LOGGER.debug("The comment on key {} does not match the spec. This may create a backup.", String.join(".", path)));

			if (config instanceof FileConfig fileConfig)
			{
				fileConfig.save();
			}
		}
		clearCache(values.valueMap().values());
	}

	// This timeout will apply once every time a config file is changed.
	//  We really want to catch an opportunity to apply it, explaining the significant 500ms timeout.
	private static final long correctionTimeoutMs = 500;
	public boolean applyCorrectionAction(CommentedFileConfig config, BiConsumer<FabricIcebergConfigSpec, CommentedFileConfig> consumer, String modId)
	{
		long startTime = System.currentTimeMillis();

		// Wait for correction to be done, up to the timeout.
		while (isCorrecting)
		{
			if (System.currentTimeMillis() - startTime > correctionTimeoutMs)
			{
				// Timeout reached, failed to correct so return false.
				return false;
			}

			try
			{
				Thread.sleep(2);
			}
			catch (InterruptedException e)
			{
				// If we weren't able to apply the corrective action at this time, just
				// return true since we'll be able to try again next time.
				Thread.currentThread().interrupt();
				return true;
			}
		}

		try
		{
			config.load();
			if (!isCorrect(config))
			{
				consumer.accept(this, config);
				config.save();
			}
		}
		catch (ParsingException ex)
		{
			return false;
		}

		// Now reload.
		clearCache(values.valueMap().values());
		ConfigEvents.RELOAD.invoker().onReload(modId);

		return true;
	}

	public synchronized boolean isCorrect(CommentedConfig config)
	{
		LinkedList<String> parentPath = new LinkedList<>();

		// The config watcher isn't properly atomic, so sometimes this method can give false negatives leading
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

	@SuppressWarnings("unchecked")
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
						specConfig.valueMap().forEach((k, v) -> newValue.valueMap().put(k, v instanceof ValueSpec<?> vSpec ? vSpec.getDefault() : v));
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
		if (obj1 instanceof String string1 && obj2 instanceof String string2)
		{
			if (string1.length() > 0 && string2.length() > 0)
			{
				return string1.replaceAll("\r\n", "\n").equals(string2.replaceAll("\r\n", "\n"));
			}
		}

		// Fallback for when we're not given Strings, or one of them is empty
		return Objects.equals(obj1, obj2);
	}

	private void clearCache(final Iterable<Object> configValues)
	{
		configValues.forEach(value ->
		{
			if (value instanceof ConfigValue<?> configValue)
			{
				configValue.clearCache();
			}
			else if (value instanceof Config innerConfig)
			{
				clearCache(innerConfig.valueMap().values());
			}
		});
	}

	public static class ConfigValue<T> implements Supplier<T>
	{
		private final Builder parent;
		private final List<String> path;
		private final Supplier<T> defaultSupplier;
		private final Class<T> clazz;

		private T cachedValue = null;

		private FabricIcebergConfigSpec spec;

		ConfigValue(Builder parent, List<String> path, Supplier<T> defaultSupplier, Class<T> clazz)
		{
			this.parent = parent;
			this.path = path;
			this.defaultSupplier = defaultSupplier;
			this.clazz = clazz;
			this.parent.values.add(Pair.of(path, this));
		}

		public List<String> getPath() { return Lists.newArrayList(path); }

		@Override
		public T get()
		{
			Objects.requireNonNull(spec, "Cannot get config value before spec is built");

			if (spec.childConfig == null)
			{
				throw new IllegalStateException("Cannot get config value before config is loaded.");
			}

			if (cachedValue == null)
			{
				cachedValue = getRaw(spec.childConfig, path, defaultSupplier);
			}

			return cachedValue;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		protected T getRaw(Config config, List<String> path, Supplier<T> defaultSupplier)
		{
			// Convert the value as needed if this is an enum.
			if (clazz.isEnum())
			{
				return (T) getRawEnum(config, path, (Class<Enum>) clazz, (Supplier<Enum>) defaultSupplier);
			}

			return config.getOrElse(path, defaultSupplier);
		}

		private <U extends Enum<U>> U getRawEnum(Config config, List<String> path, Class<U> clazz, Supplier<U> defaultSupplier)
		{
			return config.getEnumOrElse(path, (Class<U>) clazz, EnumGetMethod.NAME_IGNORECASE, defaultSupplier);
		}

		public T getDefault() { return defaultSupplier.get(); }

		public Builder next() { return parent; }

		public void save()
		{
			Objects.requireNonNull(spec, "Cannot save config value before spec is built");
			Objects.requireNonNull(spec.childConfig, "Cannot save config value without assigned Config object present");
			spec.save();
		}

		public void set(T value)
		{
			Objects.requireNonNull(spec, "Cannot set config value before spec is built");
			Objects.requireNonNull(spec.childConfig, "Cannot set config value without assigned Config object present");
			spec.childConfig.set(path, value);
			this.cachedValue = value;
		}

		public void clearCache() { this.cachedValue = null; }
	}

	public static class Builder implements IIcebergConfigSpecBuilder
	{
		private final Config storage = Config.of(LinkedHashMap::new, InMemoryFormat.withUniversalSupport());
		private BuilderContext context = new BuilderContext();
		private final Map<List<String>, String> levelComments = Maps.newHashMap();
		private final Map<List<String>, String> levelTranslationKeys = Maps.newHashMap();
		private final List<String> currentPath = Lists.newArrayList();
		private final List<Pair<List<String>, Supplier<?>>> values = Lists.newArrayList();

		@Override
		public void reset()
		{
			storage.clear();
			context = new BuilderContext();
			levelComments.clear();
			levelTranslationKeys.clear();
			currentPath.clear();
			values.clear();
		}

		@Override
		public Builder comment(String comment)
		{
			context.addComment(comment);
			return this;
		}

		@Override
		public Builder comment(String... comments)
		{
			for (String comment : comments)
			{
				comment(comment);
			}
			return this;
		}

		@Override
		public Builder translation(String translationKey)
		{
			context.setTranslationKey(translationKey);
			return this;
		}

		@Override
		public Builder push(String path)
		{
			return push(StringUtils.split(path, '.'));
		}

		@Override
		public Builder push(List<String> path)
		{
			currentPath.addAll(path);
			if (context.hasComment())
			{
				levelComments.put(Lists.newArrayList(currentPath), context.buildComment(path));
				context.clearComment();
			}

			if (context.getTranslationKey() != null)
			{
				levelTranslationKeys.put(Lists.newArrayList(currentPath), context.getTranslationKey());
				context.setTranslationKey(null);
			}
			context.ensureEmpty();
			return this;
		}

		@Override
		public Builder pop()
		{
			if (currentPath.isEmpty())
			{
				throw new IllegalArgumentException("Attempted to pop an empty config path!");
			}
			currentPath.remove(currentPath.size() - 1);
			return this;
		}

		@Override
		public <T> Pair<T, IIcebergConfigSpec> finish(Function<IIcebergConfigSpecBuilder, T> consumer)
		{
			T o = consumer.apply(this);

			context.ensureEmpty();
			Config valueCfg = Config.of(Config.getDefaultMapCreator(true, true), InMemoryFormat.withUniversalSupport());
			values.forEach(v -> valueCfg.set(v.getKey(), v.getValue()));

			FabricIcebergConfigSpec ret = new FabricIcebergConfigSpec(storage, valueCfg, levelComments, levelTranslationKeys);
			values.forEach(v -> ((ConfigValue<?>)v.getValue()).spec = ret);

			reset();

			return Pair.of(o, ret);
		}


		private <T> Supplier<T> add(List<String> path, ValueSpec<T> value, Supplier<T> defaultSupplier, Class<T> clazz)
		{
			if (!currentPath.isEmpty())
			{
				path.addAll(0, currentPath);
			}
			storage.set(path, value);
			context = new BuilderContext();
			return new ConfigValue<>(this, path, defaultSupplier, clazz);
		}

		private <T> Supplier<T> add(List<String> path, Supplier<T> defaultSupplier, Predicate<Object> validator, Class<T> clazz)
		{
			return add(path, new ValueSpec<T>(defaultSupplier, validator, context, path), defaultSupplier, clazz);
		}

		@Override
		public <T> Supplier<T> add(String path, T defaultValue)
		{
			return add(path, defaultValue, o -> o != null && defaultValue.getClass().isAssignableFrom(o.getClass()));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Supplier<T> add(String path, T defaultValue, Predicate<Object> validator)
		{
			return add(StringUtils.split(path, '.'), (Supplier<T>)(() -> defaultValue), validator, (Class<T>) defaultValue.getClass());
		}

		@Override
		public <V extends Comparable<? super V>> Supplier<V> addInRange(String path, V defaultValue, V min, V max, Class<V> clazz)
		{
			Range<V> range = new Range<V>(clazz, min, max);
			context.setRange(range, clazz);
			comment("Range: " + range);
			if (min.compareTo(max) > 0)
			{
				throw new IllegalArgumentException("Range min most be less then max.");
			}
			return add(path, defaultValue, range);
		}

		@Override
		public <T> Supplier<T> addInList(String path, T defaultValue, Collection<? extends T> acceptableValues)
		{
			return add(path, defaultValue, o -> o != null && acceptableValues.contains(o));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Supplier<List<? extends T>> addList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator)
		{
			Supplier<List<? extends T>> defaultSupplier = () -> defaultValue;
			List<String> splitPath = StringUtils.split(path, '.');

			context.setClazz(List.class);
			return add(splitPath, new ValueSpec<List<? extends T>>(defaultSupplier, x -> x instanceof List && ((List<?>) x).stream().allMatch( elementValidator ), context, splitPath)
			{
				@Override
				public List<? extends T> correct(List<? extends T> value)
				{
					if (!(value instanceof List<? extends T> list) || list.isEmpty())
					{
						Iceberg.LOGGER.debug("List on key {} is deemed to need correction. It is null, not a list, or an empty list.", splitPath.get(splitPath.size() - 1));
						return getDefault();
					}

					final List<? extends T> copy = Lists.newArrayList(list);
					copy.removeIf(elementValidator.negate());
					if (copy.isEmpty())
					{
						Iceberg.LOGGER.debug("List on key {} is deemed to need correction. It failed validation.", splitPath.get(splitPath.size() - 1));
						return getDefault();
					}
					return copy;
				}
			}, defaultSupplier, (Class<List<? extends T>>) (Class<?>) List.class);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Supplier<List<? extends T>> addListAllowEmpty(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator)
		{
			Supplier<List<? extends T>> defaultSupplier = () -> defaultValue;
			List<String> splitPath = StringUtils.split(path, '.');

			context.setClazz(List.class);
			return add(splitPath, new ValueSpec<List<? extends T>>(defaultSupplier, x -> x instanceof List && ((List<?>) x).stream().allMatch( elementValidator ), context, splitPath)
			{
				@Override
				public List<? extends T> correct(List<? extends T> value)
				{
					if (!(value instanceof List<? extends T> list))
					{
						Iceberg.LOGGER.debug("List on key {} is deemed to need correction, as it is null or not a list.", splitPath.get(splitPath.size() - 1));
						return getDefault();
					}

					final List<? extends T> copy = Lists.newArrayList(list);
					copy.removeIf(elementValidator.negate());
					if (copy.isEmpty())
					{
						Iceberg.LOGGER.debug("List on key {} is deemed to need correction. It failed validation.", splitPath.get(splitPath.size() - 1));
						return getDefault();
					}
					return copy;
				}
			}, defaultSupplier, (Class<List<? extends T>>) (Class<?>) List.class);
		}

		@Override
		public <V extends Enum<V>> Supplier<V> addEnum(String path, V defaultValue)
		{
			Class<V> clazz = defaultValue.getDeclaringClass();
			context.setClazz(clazz);

			return addEnum(path, defaultValue, o -> {
				if (o == null)
				{
					return false;
				}

				if (o instanceof Enum)
				{
					return true;
				}

				if (o instanceof String str)
				{
					return EnumUtils.isValidEnumIgnoreCase(clazz, str);
				}

				return false;
			});
		}

		@Override
		public <V extends Enum<V>> Supplier<V> addEnum(String path, V defaultValue, Predicate<Object> validator)
		{
			Supplier<V> defaultSupplier = () -> defaultValue;
			List<String> splitPath = StringUtils.split(path, '.');

			Class<V> clazz = defaultValue.getDeclaringClass();
			context.setClazz(clazz);
			V[] allowedValues = clazz.getEnumConstants();
			comment("Allowed values: " + Arrays.stream(allowedValues).filter(validator).map(Enum::name).collect(Collectors.joining(", ")));

			return new ConfigValue<V>(this, ((ConfigValue<V>)add(splitPath, new ValueSpec<V>(defaultSupplier, validator, context, splitPath), defaultSupplier, clazz)).getPath(), defaultSupplier, clazz);
		}

		@Override
		public Supplier<Boolean> add(String path, boolean defaultValue)
		{
			return add(path, (Boolean)defaultValue);
		}

		@Override
		public Supplier<Double> addInRange(String path, double defaultValue, double min, double max)
		{
			return addInRange(path, defaultValue, min, max, Double.class);
		}

		@Override
		public Supplier<Integer> addInRange(String path, int defaultValue, int min, int max)
		{
			return addInRange(path, defaultValue, min, max, Integer.class);
		}

		@Override
		public Supplier<Long> addInRange(String path, long defaultValue, long min, long max)
		{
			return addInRange(path, defaultValue, min, max, Long.class);
		}

		@Override
		public Supplier<Map<String, Object>> addSubconfig(String path, Map<String, Object> defaultValue, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			List<String> splitPath = StringUtils.split(path, '.');
			return addSubconfig(splitPath, defaultValue, keyValidator, valueValidator);
		}

		public Supplier<Map<String, Object>> addSubconfig(List<String> path, Map<String, Object> defaultValue, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			return addSubconfig(path, () -> defaultValue, keyValidator, valueValidator);
		}

		public Supplier<Map<String, Object>> addSubconfig(String path, Supplier<Map<String, Object>> defaultSupplier, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			List<String> splitPath = StringUtils.split(path, '.');
			return addSubconfig(splitPath, defaultSupplier, keyValidator, valueValidator);
		}

		public Supplier<Map<String, Object>> addSubconfig(List<String> path, Supplier<Map<String, Object>> defaultSupplier, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
		{
			final UnmodifiableConfig defaultConfig = Config.of(defaultSupplier, TomlFormat.instance());
			Supplier<Config> value = add(path, () -> MutableSubconfig.copy(defaultConfig, keyValidator, valueValidator), o -> o != null, Config.class);
			return () -> value.get().valueMap();
		}
	}

	static class BuilderContext
	{
		private final List<String> comment = Lists.newLinkedList();
		private String langKey;
		private Range<?> range;
		private boolean worldRestart = false;
		private Class<?> clazz;

		public void addComment(String value) { comment.add(value); }
		public void clearComment() { comment.clear(); }
		public boolean hasComment() { return !this.comment.isEmpty(); }
		public String buildComment() { return buildComment(List.of("unknown", "unknown")); }
		public String buildComment(final List<String> path)
		{
			if (comment.stream().allMatch(String::isBlank))
			{
				throw new IllegalStateException("Can not build comment for config option " + String.join(".", path) + " as it comprises entirely of blank lines/whitespace. This is not allowed as it causes a \"constantly correcting config\" bug with NightConfig.");
			}

			return String.join("\n", comment);
		}
		public void setTranslationKey(String value) { this.langKey = value; }
		public String getTranslationKey() { return this.langKey; }
		public <V extends Comparable<? super V>> void setRange(Range<V> value, Class<V> clazz)
		{
			this.range = value;
			this.setClazz(clazz);
		}

		@SuppressWarnings("unchecked")
		public <V extends Comparable<? super V>> Range<V> getRange() { return (Range<V>)this.range; }
		public void setClazz(Class<?> clazz) { this.clazz = clazz; }
		public Class<?> getClazz(){ return this.clazz; }

		public void ensureEmpty() {
			validate(hasComment(), "Non-empty comment when empty expected");
			validate(langKey, "Non-null translation key when null expected");
			validate(range, "Non-null range when null expected");
			validate(worldRestart, "Dangling world restart value set to true");
		}

		private void validate(Object value, String message) {
			if (value != null)
				throw new IllegalStateException(message);
		}
		private void validate(boolean value, String message) {
			if (value)
				throw new IllegalStateException(message);
		}
	}
}
