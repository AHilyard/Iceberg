package com.anthonyhilyard.iceberg.fabric.services;

import com.anthonyhilyard.iceberg.services.IKeyMappingRegistrar;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public class FabricKeyMappingRegistrar implements IKeyMappingRegistrar
{
	@Override
	public KeyMapping registerMapping(KeyMapping mapping)
	{
		return KeyBindingHelper.registerKeyBinding(mapping);
	}
}
