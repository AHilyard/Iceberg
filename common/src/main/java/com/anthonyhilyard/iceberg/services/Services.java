package com.anthonyhilyard.iceberg.services;

import java.util.ServiceLoader;

import com.anthonyhilyard.iceberg.Iceberg;

public class Services
{
	public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
	public static final IBufferSourceFactory BUFFER_SOURCE_FACTORY = load(IBufferSourceFactory.class);
	public static final IIcebergConfigSpecBuilder CONFIG_SPEC_BUILDER = load(IIcebergConfigSpecBuilder.class);
	public static final IKeyMappingRegistrar KEY_MAPPING_REGISTRAR = load(IKeyMappingRegistrar.class);
	public static final IReloadListenerRegistrar RELOAD_LISTENER_REGISTRAR = load(IReloadListenerRegistrar.class);
	public static final IFontLookup FONT_LOOKUP = load(IFontLookup.class);

	// Utilize the service loader to load platform-specific implementations of our services.
	protected static <T> T load(Class<T> clazz)
	{
		final T loadedService = ServiceLoader.load(clazz)
				.findFirst()
				.orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
		Iceberg.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
		return loadedService;
	}
}
