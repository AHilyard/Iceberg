package com.anthonyhilyard.iceberg.forge.services;

import java.util.Set;
import com.anthonyhilyard.iceberg.services.IKeyMappingRegistrar;
import com.google.common.collect.Sets;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeKeyMappingRegistrar implements IKeyMappingRegistrar
{
	private static Set<KeyMapping> keyMappings = Sets.newHashSet();

	@Override
	public KeyMapping registerMapping(KeyMapping mapping)
	{
		keyMappings.add(mapping);
		return mapping;
	}
	
	@SubscribeEvent
	public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
	{
		for (KeyMapping mapping : keyMappings)
		{
			event.register(mapping);
		}
	}
}
