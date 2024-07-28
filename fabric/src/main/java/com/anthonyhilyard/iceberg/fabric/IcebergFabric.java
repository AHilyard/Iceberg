package com.anthonyhilyard.iceberg.fabric;

import java.lang.reflect.Method;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class IcebergFabric implements PreLaunchEntrypoint
{
	private static Method registerConfig = null;
	@Override
	public void onPreLaunch()
	{
		try
		{
			registerConfig = Class.forName("com.anthonyhilyard.iceberg.fabric.config.ConfigRegistrar").getMethod("registerConfig", Class.class, IIcebergConfigSpec.class, String.class);
		}
		catch (Exception e)
		{
			Iceberg.LOGGER.error(ExceptionUtils.getStackTrace(e));
		}

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
}
