package com.anthonyhilyard.iceberg.fabric.compat;

import java.util.HashMap;

import com.anthonyhilyard.iceberg.services.IKeyMappingRegistrar.KeyMappingContext;

import committee.nova.mkb.api.IKeyBinding;
import committee.nova.mkb.api.IKeyConflictContext;
import committee.nova.mkb.keybinding.KeyConflictContext;
import net.minecraft.client.KeyMapping;

public class ModernKeyBindingHandler
{
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

	public static void setKeyMappingContext(KeyMapping mapping, KeyMappingContext context)
	{
		if (mapping instanceof IKeyBinding extendedBinding)
		{
			extendedBinding.setKeyConflictContext(contextResolver.getOrDefault(context, KeyConflictContext.UNIVERSAL));
		}
	}
}
