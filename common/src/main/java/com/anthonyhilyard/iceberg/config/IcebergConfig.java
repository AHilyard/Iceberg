package com.anthonyhilyard.iceberg.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;
import com.anthonyhilyard.iceberg.services.IIcebergConfigSpecBuilder;
import com.anthonyhilyard.iceberg.services.Services;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.exception.ExceptionUtils;
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

	public boolean isLoaded()
	{
		return configSpecs.containsKey(modId) && configSpecs.get(modId).isLoaded();
	}

	private static Method registerConfig = null;

	public static synchronized final boolean register(Class<? extends IcebergConfig<?>> subClass, @NotNull String modId)
	{
		if (registeredClasses.contains(subClass))
		{
			Iceberg.LOGGER.warn("Failed to register configuration: " + subClass.getName() + " is already registered!");
			return false;
		}

		if (ConfigEvents.REGISTER.listenerCount() == 0)
		{
			// Not my favorite solution in the world, but at least it means mod loading order isn't as important.
			String classTarget;
			switch (Services.getPlatformHelper().getPlatformName())
			{
				case "Fabric":
					classTarget = "com.anthonyhilyard.iceberg.fabric.config.ConfigRegistrar";
					break;
				case "Forge":
					classTarget = "com.anthonyhilyard.iceberg.forge.IcebergForge";
					break;
				case "NeoForge":
					classTarget = "com.anthonyhilyard.iceberg.neoforge.IcebergNeoForge";
					break;
				default:
					throw new IllegalStateException("Unable to determine modloader when registering config!");
			}

			try
			{
				registerConfig = Class.forName(classTarget).getDeclaredMethod("registerConfig", Class.class, IIcebergConfigSpec.class, String.class);
				registerConfig.setAccessible(true);
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}

			Preconditions.checkNotNull(registerConfig);

			ConfigEvents.REGISTER.register((clazz, spec, modid) -> {
				if (registerConfig != null)
				{
					try
					{
						registerConfig.invoke(null, clazz, spec, modid);
					}
					catch (Exception e)
					{
						Iceberg.LOGGER.error(ExceptionUtils.getStackTrace(e));
					}
				}
			});
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