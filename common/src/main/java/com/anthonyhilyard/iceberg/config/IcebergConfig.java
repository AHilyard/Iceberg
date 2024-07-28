package com.anthonyhilyard.iceberg.config;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;
import com.anthonyhilyard.iceberg.services.IIcebergConfigSpecBuilder;
import com.anthonyhilyard.iceberg.services.Services;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public abstract class IcebergConfig<T extends IcebergConfig<?>>
{
	private final static Map<String, IIcebergConfigSpec> configSpecs = Maps.newHashMap();
	protected final static Map<String, IcebergConfig<?>> configInstances = Maps.newHashMap();
	private final static Set<Class<?>> registeredClasses = Sets.newHashSet();

	private String modId = null;

	protected void onLoad() {}
	protected void onReload() {}

	protected final void onLoadEvent(String loadingModId)
	{
		if (modId != null && configInstances.containsKey(loadingModId) && loadingModId.contentEquals(modId))
		{
			onLoad();
		}
	}

	protected final void onReloadEvent(String reloadingModId)
	{
		if (modId != null && configInstances.containsKey(reloadingModId) && reloadingModId.contentEquals(modId))
		{
			onReload();
		}
	}

	public static synchronized final boolean register(Class<? extends IcebergConfig<?>> subClass, @NotNull String modId)
	{
		if (registeredClasses.contains(subClass))
		{
			Iceberg.LOGGER.warn("Failed to register configuration: " + subClass.getName() + " is already registered!");
			return false;
		}

		if (ConfigEvents.REGISTER.listenerCount() == 0)
		{
			Iceberg.LOGGER.warn("Failed to register configuration: " + subClass.getName() + " configuration register event has no listeners!  (Ensure mod loads AFTER Iceberg)");
			return false;
		}

		Pair<IcebergConfig<?>, IIcebergConfigSpec> specPair = Services.getConfigSpecBuilder().finish((builder) ->
		{
			IcebergConfig<?> result = null;
			try
			{
				Constructor<?> constructor = subClass.getDeclaredConstructor(IIcebergConfigSpecBuilder.class);
				constructor.setAccessible(true);
				result = (IcebergConfig<?>)constructor.newInstance(builder);
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.warn("Failed to register configuration:", e);
			}
			return result;
		});

		IIcebergConfigSpec spec = specPair.getRight();
		IcebergConfig<?> config = specPair.getLeft();

		if (spec == null)
		{
			Iceberg.LOGGER.warn("Failed to register configuration: Generated spec was null!");
			return false;
		}

		if (config == null)
		{
			Iceberg.LOGGER.warn("Failed to register configuration: Generated configuration instance was null!");
			return false;
		}

		config.modId = modId;

		configInstances.put(modId, config);
		configSpecs.put(modId, spec);

		ConfigEvents.LOAD.register(config::onLoadEvent);
		ConfigEvents.RELOAD.register(config::onReloadEvent);
		ConfigEvents.REGISTER.invoker().onRegister(subClass, spec, modId);

		registeredClasses.add(subClass);

		Services.getConfigSpecBuilder().reset();
		return true;
	}
}