package com.anthonyhilyard.iceberg.fabric.services;

import com.anthonyhilyard.iceberg.services.IKeyMappingRegistrar;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class FabricKeyMappingRegistrar implements IKeyMappingRegistrar
{
	@Override
	public KeyMapping registerMapping(KeyMapping mapping)
	{
		return KeyMappingHelper.registerKeyMapping(mapping);
	}
}
