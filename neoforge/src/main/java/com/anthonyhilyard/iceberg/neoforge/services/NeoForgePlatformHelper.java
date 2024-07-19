package com.anthonyhilyard.iceberg.neoforge.services;

import java.util.List;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import com.anthonyhilyard.iceberg.services.IPlatformHelper;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

public class NeoForgePlatformHelper implements IPlatformHelper
{

	@Override
	public String getPlatformName() { return "NeoForge"; }

	@Override
	public boolean isModLoaded(String modId) { return getAllModIds().contains(modId); }

	@Override
	public List<String> getAllModIds()
	{
		if (ModList.get() != null)
		{
			return ModList.get().applyForEachModContainer(mod -> mod.getModId()).toList();
		}
		else
		{
			return LoadingModList.get().getMods().stream().map(ModInfo::getModId).toList();
		}
	}

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
				result = ModList.get().getModContainerById(modId).get().getModInfo().getVersion().compareTo(new DefaultArtifactVersion(versionString)) >= 0;
			}
			catch (Exception e) {}
		}
		
		return result;
	}
}
