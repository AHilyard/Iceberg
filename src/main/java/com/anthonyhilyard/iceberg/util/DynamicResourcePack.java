package com.anthonyhilyard.iceberg.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.util.ResourceLocation;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.data.IMetadataSectionSerializer;

/**
 * DynamicResourcePack allows resources that are defined arbitrarily to do cool things with resources.
 * For example, resources that change in response to game state, resource proxies, or resources downloaded from the internet.
 */
public class DynamicResourcePack implements IResourcePack
{
	private static class DynamicResourceKey
	{
		public final String type;
		public final String namespace;
		public final String path;

		public DynamicResourceKey(String type, String namespace, String path)
		{
			this.type = type;
			this.namespace = namespace;
			this.path = path;
		}
	}

	private final String packName;
	private Map<DynamicResourceKey, Supplier<InputStream>> dynamicResourceMap = new HashMap<DynamicResourceKey, Supplier<InputStream>>();

	public DynamicResourcePack(String packName)
	{
		this.packName = packName;
	}

	public void clear()
	{
		dynamicResourceMap.clear();
	}

	public boolean removeResource(ResourcePackType type, ResourceLocation location)
	{
		DynamicResourceKey key = new DynamicResourceKey(type.getDirectory(), location.getNamespace(), location.getPath());
		if (dynamicResourceMap.containsKey(key))
		{
			dynamicResourceMap.remove(key);
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean registerResource(ResourcePackType type, ResourceLocation location, Supplier<InputStream> resourceSupplier)
	{
		return register(type.getDirectory(), location.getNamespace(), location.getPath(), resourceSupplier);
	}

	public boolean registerRootResource(String path, Supplier<InputStream> resourceSupplier)
	{
		return register("root", "", path, resourceSupplier);
	}

	private boolean register(String directory, String namespace, String path, Supplier<InputStream> resourceSupplier)
	{
		DynamicResourceKey key = new DynamicResourceKey(directory, namespace, path);
		if (!dynamicResourceMap.containsKey(key))
		{	
			dynamicResourceMap.put(key, resourceSupplier);
			return true;
		}
		return false;
	}

	@Override
	public InputStream getRootResource(String path) throws IOException
	{
		return getResource("root", "", path);
	}

	@Override
	public InputStream getResource(ResourcePackType type, ResourceLocation location) throws IOException
	{
		return getResource(type.getDirectory(), location.getNamespace(), location.getPath());
	}

	private InputStream getResource(String directory, String namespace, String path) throws IOException
	{
		DynamicResourceKey key = new DynamicResourceKey(directory, namespace, path);
		if (dynamicResourceMap.containsKey(key))
		{
			return dynamicResourceMap.get(key).get();
		}
		else
		{
			throw new FileNotFoundException("Can't find dynamic resource " + path + ". Please ensure it has been registered.");
		}
	}

	@Override
	public Collection<ResourceLocation> getResources(ResourcePackType type, String namespace, String path, int maxDepth, Predicate<String> filter)
	{
		return dynamicResourceMap.entrySet().stream()
		.filter(entry -> entry.getKey().namespace.contentEquals(namespace))
		.filter(entry -> entry.getKey().path.startsWith(path))
		.filter(entry -> entry.getKey().type.contentEquals(type.getDirectory()))
		.filter(entry -> filter.test(entry.getKey().path))
		.map(entry -> new ResourceLocation(namespace, entry.getKey().path))
		.collect(Collectors.toList());
	}

	@Override
	public boolean hasResource(ResourcePackType type, ResourceLocation location)
	{
		return dynamicResourceMap.containsKey(new DynamicResourceKey(type.getDirectory(), location.getNamespace(), location.getPath()));
	}

	@Override
	public Set<String> getNamespaces(ResourcePackType type)
	{
		Set<String> namespaces = new HashSet<>();
		for (DynamicResourceKey key : dynamicResourceMap.keySet())
		{
			if (type.getDirectory().contentEquals(key.type))
			{
				namespaces.add(key.namespace);
			}
		}
		return namespaces;
	}

	@Override
	public <T> T getMetadataSection(IMetadataSectionSerializer<T> p_10291_) throws IOException
	{
		// Does nothing for now.
		// TODO: Add metadata?  Probably not needed right?
		return null;
	}

	@Override
	public String getName()
	{
		return packName;
	}

	@Override
	public void close()
	{
	}
}