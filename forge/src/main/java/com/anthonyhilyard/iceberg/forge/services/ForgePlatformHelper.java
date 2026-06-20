package com.anthonyhilyard.iceberg.forge.services;

import java.util.List;
import java.util.Optional;

import net.minecraftforge.fml.ModContainer;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import com.anthonyhilyard.iceberg.services.IPlatformHelper;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

public class ForgePlatformHelper implements IPlatformHelper
{
	private List<String> cachedModList = null;

	@Override
	public String getPlatformName() { return "Forge"; }

	@Override
	public boolean isModLoaded(String modId)
	{
		if (modId == null || modId.isEmpty())
		{
			return false;
		}

		try
		{
			return ModList.isLoaded(modId);

		}
		catch(Exception ignored)
		{
			return LoadingModList.getModFileById(modId) != null;
		}
	}

	@Override
	public List<String> getAllModIds()
	{
		if (cachedModList == null)
		{
			try
			{
				cachedModList = ModList.applyForEachModContainer(mod -> mod.getModId()).toList();
			}
			catch(Exception ignored)
			{
				cachedModList = LoadingModList.getMods().stream().map(ModInfo::getModId).toList();
			}
		}

		return cachedModList;
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
				Optional<? extends ModContainer> opt = ModList.getModContainerById(modId);
				if (opt.isPresent())
				{
					result = opt.get().getModInfo().getVersion().compareTo(new DefaultArtifactVersion(versionString)) >= 0;
				}
			}
			catch (Exception e) {}
		}
		
		return result;
	}
}
