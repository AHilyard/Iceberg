package com.anthonyhilyard.iceberg.services;

import net.minecraft.client.KeyMapping;

public interface IKeyMappingRegistrar
{
	public enum KeyMappingContext
	{
		UNIVERSAL,	 // Conflicts with any other key mapping.
		GUI,		 // Only conflicts with other GUI mappings.
		IN_GAME,	 // Only conflicts with other in-game mappings.
		NO_CONFLICT  // Does not conflict with any other mapping.
	}

	KeyMapping registerMapping(KeyMapping mapping);
	KeyMapping registerMapping(KeyMapping mapping, KeyMappingContext context);
}
