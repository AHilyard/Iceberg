package com.anthonyhilyard.iceberg.services;

import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;

import com.anthonyhilyard.iceberg.Iceberg;

public class Services
{
	private static final ConcurrentHashMap<Class<?>, Supplier<?>> serviceCache = new ConcurrentHashMap<>();

	public static IPlatformHelper getPlatformHelper() { return (IPlatformHelper) serviceCache.computeIfAbsent(IPlatformHelper.class, x -> createLazySupplier(x)).get(); }
	public static IBufferSourceFactory getBufferSourceFactory() { return (IBufferSourceFactory) serviceCache.computeIfAbsent(IBufferSourceFactory.class, x -> createLazySupplier(x)).get(); }
	public static IIcebergConfigSpecBuilder getConfigSpecBuilder() { return (IIcebergConfigSpecBuilder) serviceCache.computeIfAbsent(IIcebergConfigSpecBuilder.class, x -> createLazySupplier(x)).get(); }
	public static IKeyMappingRegistrar getKeyMappingRegistrar() { return (IKeyMappingRegistrar) serviceCache.computeIfAbsent(IKeyMappingRegistrar.class, x -> createLazySupplier(x)).get(); }
	public static IReloadListenerRegistrar getReloadListenerRegistrar() { return (IReloadListenerRegistrar) serviceCache.computeIfAbsent(IReloadListenerRegistrar.class, x -> createLazySupplier(x)).get(); }
	public static IFontLookup getFontLookup() { return (IFontLookup) serviceCache.computeIfAbsent(IFontLookup.class, x -> createLazySupplier(x)).get(); }


	// Utilize the service loader to load platform-specific implementations of our services.
	protected static <T> T load(Class<T> clazz)
	{
		final T loadedService = ServiceLoader.load(clazz)
				.findFirst()
				.orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
		Iceberg.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
		return loadedService;
	}

	private static <T> Supplier<T> createLazySupplier(Class<T> clazz)
	{
		return new Supplier<T>() {
			private volatile T instance;

			@Override
			public T get()
			{
				if (instance == null)
				{
					synchronized (this)
					{
						if (instance == null)
						{
							instance = load(clazz);
						}
					}
				}
				return instance;
			}
		};
	}
}
