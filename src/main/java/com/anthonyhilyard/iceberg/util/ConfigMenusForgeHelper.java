package com.anthonyhilyard.iceberg.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.anthonyhilyard.iceberg.Loader;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.Maps;
import com.google.common.base.Objects;
import com.google.common.collect.Iterators;

import org.apache.commons.lang3.exception.ExceptionUtils;

import fuzs.configmenusforge.client.gui.data.EntryData;
import fuzs.configmenusforge.client.gui.data.IEntryData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.unsafe.UnsafeHacks;

public class ConfigMenusForgeHelper
{
	private final static Map<Class<?>, List<MethodHandle>> configSpecMethodHandles = Maps.newHashMap();
	private final static Map<Class<?>, Boolean> cachedConfigSpecClasses = Maps.newHashMap();
	private final static MethodType getValuesMethodType = MethodType.methodType(UnmodifiableConfig.class);
	private final static MethodType isLoadedMethodType = MethodType.methodType(boolean.class);
	private final static MethodType saveMethodType = MethodType.methodType(void.class);

	private static Object callMethod(UnmodifiableConfig spec, int methodIndex)
	{
		Class<?> specClass = spec.getClass();
		if (!cachedConfigSpecClasses.containsKey(specClass))
		{
			cacheClass(specClass);
		}

		if (configSpecMethodHandles.containsKey(specClass))
		{
			try
			{
				return configSpecMethodHandles.get(specClass).get(methodIndex).invoke(spec);
			}
			catch (Throwable e)
			{
				Loader.LOGGER.warn(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	public static UnmodifiableConfig getValues(UnmodifiableConfig spec)
	{
		return (UnmodifiableConfig) callMethod(spec, 0);
	}

	public static boolean isLoaded(UnmodifiableConfig spec)
	{
		return (Boolean) callMethod(spec, 1);
	}

	public static void save(UnmodifiableConfig spec)
	{
		callMethod(spec, 2);
	}

	public static Boolean cachedValidity(Class<?> specClass)
	{
		return cachedConfigSpecClasses.getOrDefault(specClass, null);
	}

	public static void cacheClass(Class<?> specClass)
	{
		MethodHandle getValuesMethod = null;
		MethodHandle isLoadedMethod = null;
		MethodHandle saveMethod = null;
		try
		{
			getValuesMethod = MethodHandles.lookup().findVirtual(specClass, "getValues", getValuesMethodType);
			isLoadedMethod = MethodHandles.lookup().findVirtual(specClass, "isLoaded", isLoadedMethodType);
			saveMethod = MethodHandles.lookup().findVirtual(specClass, "save", saveMethodType);
		}
		catch (Throwable e)
		{
			Loader.LOGGER.warn(ExceptionUtils.getStackTrace(e));
		}

		// If we found valid getValues, isLoaded and save methods, add them to the cache.
		if (getValuesMethod != null && isLoadedMethod != null && saveMethod != null)
		{
			cachedConfigSpecClasses.put(specClass, true);
			configSpecMethodHandles.put(specClass, List.of(getValuesMethod, isLoadedMethod, saveMethod));
		}
		else
		{
			cachedConfigSpecClasses.put(specClass, false);
		}
	}

	/**
	 * Changed spec from a ForgeConfigSpec to an UnmodifiableConfig.
	 */
	public static void makeValueToDataMap(UnmodifiableConfig spec, UnmodifiableConfig values, CommentedConfig comments, Map<Object, IEntryData> allData, String basePath)
	{
		for (String path : values.valueMap().keySet())
		{
			String currentPath = basePath.isEmpty() ? path : basePath + "." + path;
			Object value = values.valueMap().get(path);
			if (value instanceof UnmodifiableConfig category)
			{
				final EntryData.CategoryEntryData data = new EntryData.CategoryEntryData(path, category, comments.getComment(path));
				allData.put(category, data);
				makeValueToDataMap(spec, category, (CommentedConfig) comments.valueMap().get(path), allData, currentPath);
			}
			else if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue && configValue.get() instanceof UnmodifiableConfig category)
			{
				final EntryData.CategoryEntryData data = new DynamicCategoryEntryData(path, category, comments.getComment(path));
				allData.put(category, data);
				makeValueToDataMap(spec, category, (CommentedConfig) comments.valueMap().get(path), allData, currentPath);
			}
			else if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue)
			{
				final EntryData.ConfigEntryData<?> data = new EntryData.ConfigEntryData<>(path, configValue, spec.getRaw(configValue.getPath()));
				allData.put(configValue, data);
			}
			// Allow non-configvalue values if the parent is a dynamic subconfig.
			else if (!(value instanceof ForgeConfigSpec.ConfigValue<?>) && allData.containsKey(values) && allData.get(values) instanceof DynamicCategoryEntryData)
			{
				final EntryData.ConfigEntryData<?> data = new DynamicConfigEntryData<>(List.of(currentPath.split("\\.")), value, spec.getRaw(currentPath), spec);
				allData.put(value, data);
			}
		}
	}

	public static class DynamicCategoryEntryData extends EntryData.CategoryEntryData
	{
		public DynamicCategoryEntryData(String path, UnmodifiableConfig config, String comment) {
			super(path, config, comment);
		}
	}

	public static class DynamicConfigEntryData<T> extends EntryData.ConfigEntryData<T>
	{
		private final ForgeConfigSpec.ValueSpec valueSpec;
		private T currentValue;
		private T configValue;
		private final List<String> fullPath;
		private final UnmodifiableConfig spec;

		private final static ForgeConfigSpec.ConfigValue<?> dummyConfigValue;

		private Component title;

		static
		{
			dummyConfigValue = UnsafeHacks.newInstance(ForgeConfigSpec.ConfigValue.class);
			try
			{
				Field specField = ForgeConfigSpec.ConfigValue.class.getDeclaredField("spec");
				UnsafeHacks.setField(specField, dummyConfigValue, UnsafeHacks.newInstance(ForgeConfigSpec.class));
				Field defaultSupplierField = ForgeConfigSpec.ConfigValue.class.getDeclaredField("defaultSupplier");
				UnsafeHacks.setField(defaultSupplierField, dummyConfigValue, (Supplier<?>)(() -> null));
			}
			catch (Exception e) { }
		}

		@SuppressWarnings("unchecked")
		public DynamicConfigEntryData(List<String> fullPath, T configValue, ForgeConfigSpec.ValueSpec valueSpec, UnmodifiableConfig spec)
		{
			super(fullPath.get(fullPath.size() - 1), (ForgeConfigSpec.ConfigValue<T>) dummyConfigValue, valueSpec);
			this.configValue = configValue;
			this.currentValue = configValue;
			this.valueSpec = valueSpec;
			this.fullPath = fullPath;
			this.spec = spec;

			// We will override the normal title functionality since we want it to be unformatted.
			this.title = new TextComponent(getPath());
		}

		@Override
		public Component getTitle()
		{
			return this.title;
		}

		@Override
		public boolean mayResetValue()
		{
			return !listSafeEquals(currentValue, getDefaultValue());
		}

		@Override
		public boolean mayDiscardChanges()
		{
			return listSafeEquals(configValue, currentValue);
		}

		private static <T> boolean listSafeEquals(T o1, T o2)
		{
			// Attempts to solve an issue where types of lists won't match when one is read from file
			// (due to enum being converted to string, long to int)
			if (o1 instanceof List<?> list1 && o2 instanceof List<?> list2)
			{
				final Stream<String> stream1 = list1.stream().map(o -> o instanceof Enum<?> e ? e.name() : o.toString());
				final Stream<String> stream2 = list2.stream().map(o -> o instanceof Enum<?> e ? e.name() : o.toString());
				return Iterators.elementsEqual(stream1.iterator(), stream2.iterator());
			}
			return Objects.equal(o1, o2);
		}

		@Override
		public void resetCurrentValue()
		{
			currentValue = getDefaultValue();
		}

		@Override
		public void discardCurrentValue()
		{
			currentValue = configValue;
		}

		@Override
		public void saveConfigValue()
		{
			try
			{
				Field childConfigField = spec.getClass().getDeclaredField("childConfig");
				Config childConfig = UnsafeHacks.getField(childConfigField, spec);
				childConfig.set(fullPath, currentValue);
			}
			catch (Exception e) { }
			configValue = currentValue;
		}

		@SuppressWarnings("unchecked")
		public T getDefaultValue()
		{
			return (T) valueSpec.getDefault();
		}

		public T getCurrentValue()
		{
			return currentValue;
		}

		public void setCurrentValue(T newValue)
		{
			currentValue = newValue;
		}

		@Override
		public List<String> getFullPath()
		{
			return fullPath;
		}
	}
}