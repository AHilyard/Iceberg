package com.anthonyhilyard.iceberg.neoforge.services;

import java.util.Set;
import com.anthonyhilyard.iceberg.services.IKeyMappingRegistrar;
import com.google.common.collect.Sets;

import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public class NeoForgeKeyMappingRegistrar implements IKeyMappingRegistrar
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
