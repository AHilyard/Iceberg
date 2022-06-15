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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;

/**
 * DynamicResourcePack allows resources that are defined arbitrarily to do cool things with resources.
 * For example, resources that change in response to game state, resource proxies, or resources downloaded from the internet.
 */
public class DynamicResourcePack implements PackResources
{
	private record DynamicResourceKey(String type, String namespace, String path) {}

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

	public boolean removeResource(PackType type, ResourceLocation location)
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

	public boolean registerResource(PackType type, ResourceLocation location, Supplier<InputStream> resourceSupplier)
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
	public InputStream getResource(PackType type, ResourceLocation location) throws IOException
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
	public Collection<ResourceLocation> getResources(PackType type, String namespace, String path, Predicate<ResourceLocation> filter)
	{
		return dynamicResourceMap.entrySet().stream()
		.filter(entry -> entry.getKey().namespace.contentEquals(namespace))
		.filter(entry -> entry.getKey().path.startsWith(path))
		.filter(entry -> entry.getKey().type.contentEquals(type.getDirectory()))
		.filter(entry -> filter.test(new ResourceLocation(entry.getKey().namespace, entry.getKey().path)))
		.map(entry -> new ResourceLocation(namespace, entry.getKey().path))
		.collect(Collectors.toList());
	}

	@Override
	public boolean hasResource(PackType type, ResourceLocation location)
	{
		return dynamicResourceMap.containsKey(new DynamicResourceKey(type.getDirectory(), location.getNamespace(), location.getPath()));
	}

	@Override
	public Set<String> getNamespaces(PackType type)
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
	public <T> T getMetadataSection(MetadataSectionSerializer<T> p_10291_) throws IOException
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
