package com.anthonyhilyard.iceberg.neoforge.config;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.Map;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.util.UnsafeUtil;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.common.ModConfigSpec.LongValue;

public class ConfigValueWrapper<T, S extends ConfigValue<T>> implements Supplier<T>
{
	private final S configValue;
	private static Map<Class<?>, Field> pathFields = Maps.newHashMap();
	private static Map<Class<?>, Field> defaultSupplierFields = Maps.newHashMap();
	private static Map<Class<?>, Field> cachedValueFields = Maps.newHashMap();
	private static Map<Class<?>, Field> specFields = Maps.newHashMap();

	public ConfigValueWrapper(S configValue)
	{
		this.configValue = configValue;
	}

	private static Field findField(Class<?> startClass, String fieldName)
	{
		Class<?> currentClass = startClass;

		while (currentClass != null)
		{
			try
			{
				Field field = currentClass.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field;
			}
			catch (NoSuchFieldException e)
			{
				currentClass = currentClass.getSuperclass();
			}
		}
		return null;
	}

	private <U extends Enum<U>> U getRawEnum(Config config, List<String> path, Class<U> clazz, Supplier<U> defaultSupplier)
	{
		return config.getEnumOrElse(path, (Class<U>) clazz, EnumGetMethod.NAME_IGNORECASE, defaultSupplier);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public T get()
	{
		Class<?> configValueClass = configValue.getClass();
		if (!pathFields.containsKey(configValueClass))
		{
			try
			{
				pathFields.put(configValueClass, findField(configValueClass, "path"));
				defaultSupplierFields.put(configValueClass, findField(configValueClass, "defaultSupplier"));
				cachedValueFields.put(configValueClass, findField(configValueClass, "cachedValue"));
				specFields.put(configValueClass, findField(configValueClass, "spec"));
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}

		if (pathFields.containsKey(configValueClass))
		{
			try
			{
				T cachedValue = (T) cachedValueFields.get(configValueClass).get(configValue);
				if (cachedValue == null)
				{
					NeoForgeIcebergConfigSpec spec = (NeoForgeIcebergConfigSpec) UnsafeUtil.getField(specFields.get(configValueClass), configValue);
					Preconditions.checkState(spec != null, "Cannot get config value before spec is built.");
					Preconditions.checkState(spec.loadedConfig() != null, "Cannot get config value before config is loaded.");

					List<String> path = (List<String>) UnsafeUtil.getField(pathFields.get(configValueClass), configValue);
					Supplier<T> defaultSupplier = (Supplier<T>) UnsafeUtil.getField(defaultSupplierFields.get(configValueClass), configValue);
					
					Object value = spec.loadedConfig().config().getOrElse(path, defaultSupplier);

					if (configValueClass == EnumValue.class)
					{
						Field clazzField = EnumValue.class.getDeclaredField("clazz");
						clazzField.setAccessible(true);

						value = getRawEnum(spec.loadedConfig().config(), path, (Class<Enum>)UnsafeUtil.getField(clazzField, configValue), (Supplier<Enum>) defaultSupplier);
					}

					if (value instanceof Number number)
					{
						if (configValueClass == IntValue.class)
						{
							value = number.intValue();
						}
						else if (configValueClass == LongValue.class)
						{
							value = number.longValue();
						}
						else if (configValueClass == DoubleValue.class)
						{
							value = number.doubleValue();
						}
					}
					cachedValue = (T)value;
					UnsafeUtil.setField(cachedValueFields.get(configValueClass), configValue, cachedValue);
				}

				return cachedValue;
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}

		Preconditions.checkState(false);
		return null;
	}
	
}
