package com.anthonyhilyard.iceberg.fabric;

import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.config.IcebergConfig;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;
import com.anthonyhilyard.iceberg.fabric.config.ConfigTracker;
import com.anthonyhilyard.iceberg.fabric.config.FabricIcebergConfigSpec;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class IcebergFabric implements PreLaunchEntrypoint
{
	@Override
	public void onPreLaunch()
	{
		ConfigEvents.REGISTER.register(this::registerConfig);
	}

	private void registerConfig(Class<? extends IcebergConfig<?>> clazz, IIcebergConfigSpec spec, String modid)
	{
		ConfigTracker.INSTANCE.registerConfig((FabricIcebergConfigSpec)spec, modid);
	}
}
