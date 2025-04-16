package com.anthonyhilyard.iceberg.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.anthonyhilyard.iceberg.services.Services;

public class MixinConfig implements IMixinConfigPlugin
{
	private static String packageName = MixinConfig.class.getPackage().getName();
	@Override
	public void onLoad(String mixinPackage) { }

	@Override
	public String getRefMapperConfig() { return null; }

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		// If a mixin is in a subfolder, assume it is a mod ID and check if that mod is loaded.
		String subPackage = mixinClassName.substring(packageName.length() + 1);

		int dotIndex = subPackage.indexOf('.');
		if (dotIndex > 0)
		{
			String modId = subPackage.substring(0, dotIndex);
			return Services.getPlatformHelper().isModLoaded(modId);
		}

		// If there's no mod ID, default to applying the mixin.
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