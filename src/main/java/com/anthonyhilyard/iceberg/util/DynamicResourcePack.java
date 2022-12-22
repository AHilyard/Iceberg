package com.anthonyhilyard.iceberg.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

/**
 * DynamicResourcePack allows resources that are defined arbitrarily to do cool things with resources.
 * For example, resources that change in response to game state, resource proxies, or resources downloaded from the internet.
 */
public class DynamicResourcePack implements PackResources
{
	private record DynamicResourceKey(String type, String namespace, String path) {}

	private final String packName;
	private Map<DynamicResourceKey, IoSupplier<InputStream>> dynamicResourceMap = new HashMap<DynamicResourceKey, IoSupplier<InputStream>>();

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

	public boolean registerResource(PackType type, ResourceLocation location, IoSupplier<InputStream> resourceSupplier)
	{
		return register(type.getDirectory(), location.getNamespace(), location.getPath(), resourceSupplier);
	}

	public boolean registerRootResource(String path, IoSupplier<InputStream> resourceSupplier)
	{
		return register("root", "", path, resourceSupplier);
	}

	private boolean register(String directory, String namespace, String path, IoSupplier<InputStream> resourceSupplier)
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
	@Nullable
	public IoSupplier<InputStream> getRootResource(String... path)
	{
		try
		{
			return getResource("root", "", String.join("/", path));
		}
		catch (IOException e)
		{
			return null;
		}
	}

	@Override
	@Nullable
	public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location)
	{
		try
		{
			return getResource(type.getDirectory(), location.getNamespace(), location.getPath());
		}
		catch (IOException e)
		{
			return null;
		}
	}

	private IoSupplier<InputStream> getResource(String directory, String namespace, String path) throws IOException
	{
		DynamicResourceKey key = new DynamicResourceKey(directory, namespace, path);
		if (dynamicResourceMap.containsKey(key))
		{
			return dynamicResourceMap.get(key);
		}
		else
		{
			throw new FileNotFoundException("Can't find dynamic resource " + path + ". Please ensure it has been registered.");
		}
	}

	@Override
	public void listResources(PackType type, String namespace, String path, ResourceOutput output)
	{
		dynamicResourceMap.entrySet().stream()
		.filter(entry -> entry.getKey().namespace.contentEquals(namespace))
		.filter(entry -> entry.getKey().path.startsWith(path))
		.filter(entry -> entry.getKey().type.contentEquals(type.getDirectory()))
		.forEach(entry -> output.accept(new ResourceLocation(namespace, entry.getKey().path), entry.getValue()));
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
	public String packId()
	{
		return packName;
	}

	@Override
	public void close()
	{
	}
}
