package com.anthonyhilyard.iceberg.neoforge.services;

import java.util.HashMap;
import java.util.Set;

import com.anthonyhilyard.iceberg.services.IKeyMappingRegistrar;

import com.google.common.collect.Sets;

import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public class NeoForgeKeyMappingRegistrar implements IKeyMappingRegistrar
{
	private static Set<KeyMapping> keyMappings = Sets.newHashSet();

	private static final IKeyConflictContext noConflictContext = new IKeyConflictContext()
		{
			@Override
			public boolean isActive() { return true; }

			@Override
			public boolean conflicts(IKeyConflictContext other) { return false; }
		};

	private static final HashMap<KeyMappingContext, IKeyConflictContext> contextResolver = new HashMap<>() {
		{
			put(KeyMappingContext.UNIVERSAL, KeyConflictContext.UNIVERSAL);
			put(KeyMappingContext.GUI, KeyConflictContext.GUI);
			put(KeyMappingContext.IN_GAME, KeyConflictContext.IN_GAME);
			put(KeyMappingContext.NO_CONFLICT, noConflictContext);
		}
	};

	@Override
	public KeyMapping registerMapping(KeyMapping mapping)
	{
		keyMappings.add(mapping);
		return mapping;
	}

	@Override
	public KeyMapping registerMapping(KeyMapping mapping, KeyMappingContext context)
	{
		mapping.setKeyConflictContext(contextResolver.getOrDefault(context, KeyConflictContext.UNIVERSAL));
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
