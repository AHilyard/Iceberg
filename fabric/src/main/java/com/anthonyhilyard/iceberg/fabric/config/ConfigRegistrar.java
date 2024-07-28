package com.anthonyhilyard.iceberg.fabric.config;

import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.config.IcebergConfig;

public class ConfigRegistrar
{
	public static void registerConfig(Class<? extends IcebergConfig<?>> clazz, IIcebergConfigSpec spec, String modid)
	{
		ConfigTracker.INSTANCE.registerConfig((FabricIcebergConfigSpec)spec, modid);
	}
}
