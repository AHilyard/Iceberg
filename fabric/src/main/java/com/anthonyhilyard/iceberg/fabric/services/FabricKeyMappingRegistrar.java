package com.anthonyhilyard.iceberg.fabric.services;

import com.anthonyhilyard.iceberg.services.IKeyMappingRegistrar;
import com.anthonyhilyard.iceberg.services.Services;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public class FabricKeyMappingRegistrar implements IKeyMappingRegistrar
{
	@Override
	public KeyMapping registerMapping(KeyMapping mapping) { return KeyBindingHelper.registerKeyBinding(mapping); }

	@Override
	public KeyMapping registerMapping(KeyMapping mapping, KeyMappingContext context)
	{
		if (Services.getPlatformHelper().isModLoaded("mkb"))
		{
			try
			{
				Class.forName("com.anthonyhilyard.iceberg.fabric.compat.ModernKeyBindingHandler").getDeclaredMethod("setKeyMappingContext", KeyMapping.class, KeyMappingContext.class).invoke(null, mapping, context);
			}
			catch (Exception e)
			{
				// Welp, we tried.  Do nothing.
			}
		}
		return registerMapping(mapping);
	}
}
