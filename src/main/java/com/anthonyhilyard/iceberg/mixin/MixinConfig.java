package com.anthonyhilyard.iceberg.mixin;

import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

public class MixinConfig implements IMixinConfigPlugin
{
	private LoadingModList loadingModList = null;

	@Override
	public void onLoad(String mixinPackage) { }

	@Override
	public String getRefMapperConfig() { return null; }

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		if (mixinClassName.toLowerCase().contains("configmenusforge"))
		{
			if (loadingModList == null)
			{
				loadingModList = FMLLoader.getLoadingModList();
			}

			// Check if Config Menus for Forge is available.
			for (ModInfo modInfo : loadingModList.getMods())
			{
				// If config menus for forge is loaded AND it is version 3.1.0, load our mixins.
				if (modInfo.getModId().equals("configmenusforge") && modInfo.getVersion().compareTo(new DefaultArtifactVersion("3.1.0")) == 0)
				{
					return true;
				}
			}
			
			return false;
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

	@Override
	public List<String> getMixins() { return null; }

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}