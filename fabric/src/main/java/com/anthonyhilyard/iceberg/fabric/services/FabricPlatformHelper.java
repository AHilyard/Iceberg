package com.anthonyhilyard.iceberg.fabric.services;

import java.util.List;

import com.anthonyhilyard.iceberg.services.IPlatformHelper;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public class FabricPlatformHelper implements IPlatformHelper
{
	@Override
	public String getPlatformName() { return "Fabric"; }

	@Override
	public boolean isModLoaded(String modId) { return FabricLoader.getInstance().isModLoaded(modId); }

	@Override
	public List<String> getAllModIds() { return FabricLoader.getInstance().getAllMods().stream().map(mod -> mod.getMetadata().getId()).toList(); }

	@Override
	public boolean modVersionMeets(String modId, String versionString)
	{
		// The version string should contain only a version.
		if (versionString.contains("<") || versionString.contains(">") || versionString.contains("=") || versionString.contains("~"))
		{
			return false;
		}

		boolean result = false;

		// If the mod is loaded, test the version using the VersionPredicate parser.
		if (isModLoaded(modId))
		{
			try
			{
				result = VersionPredicate.parse(">=" + versionString).test(FabricLoader.getInstance().getModContainer(modId).get().getMetadata().getVersion());
			}
			catch (Exception e) {}
		}
		
		return result;
	}
}
