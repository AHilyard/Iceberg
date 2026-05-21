package com.anthonyhilyard.iceberg.forge.services;

import java.util.Set;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.services.IKeyMappingRegistrar;
import com.google.common.collect.Sets;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Iceberg.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
