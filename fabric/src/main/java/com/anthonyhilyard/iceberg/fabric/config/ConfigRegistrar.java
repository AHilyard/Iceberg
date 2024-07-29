package com.anthonyhilyard.iceberg.fabric.config;

import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.config.IcebergConfig;

public class ConfigRegistrar
{
	@SuppressWarnings("unused")
	private static void registerConfig(Class<? extends IcebergConfig<?>> clazz, IIcebergConfigSpec spec, String modid)
	{
		ConfigTracker.INSTANCE.registerConfig((FabricIcebergConfigSpec)spec, modid);
	}
}
