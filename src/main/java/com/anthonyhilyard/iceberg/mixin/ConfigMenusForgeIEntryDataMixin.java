package com.anthonyhilyard.iceberg.mixin;

import java.util.Map;

import com.anthonyhilyard.iceberg.util.ConfigMenusForgeHelper;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fuzs.configmenusforge.client.gui.data.IEntryData;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

@Mixin(IEntryData.class)
public interface ConfigMenusForgeIEntryDataMixin
{
	/**
	 * @author Iceberg
	 * @reason Overwriting makeValueToDataMap to allow classes other than ForgeConfigSpec to be supported.
	 */
	@Overwrite(remap = false)
	public static Map<Object, IEntryData> makeValueToDataMap(ModConfig config)
	{
		if (checkInvalid(config))
		{
			return ImmutableMap.of();
		}
		Map<Object, IEntryData> allData = Maps.newHashMap();
		UnmodifiableConfig spec = config.getSpec();
		ConfigMenusForgeHelper.makeValueToDataMap(spec, ConfigMenusForgeHelper.getValues(spec), config.getConfigData(), allData, "");
		return ImmutableMap.copyOf(allData);
	}

	/**
	 * @author Iceberg
	 * @reason Overwriting checkInvalid to allow classes other than ForgeConfigSpec to be supported.
	 */
	@Overwrite(remap = false)
	public static boolean checkInvalid(ModConfig config)
	{
		IConfigSpec<?> spec = config.getSpec();

		// True / false means the config class has been cached, null means it's new.
		Boolean cachedValue = ConfigMenusForgeHelper.cachedValidity(spec.getClass());
		if (cachedValue == null)
		{
			// It's not cached, so do the lookup via MethodHandles API and cache the results.
			ConfigMenusForgeHelper.cacheClass(spec.getClass());
		}

		return config.getConfigData() == null || !ConfigMenusForgeHelper.cachedValidity(spec.getClass()) || !ConfigMenusForgeHelper.isLoaded(spec);
	}
}
