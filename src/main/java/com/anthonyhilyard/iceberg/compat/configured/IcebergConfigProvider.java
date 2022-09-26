package com.anthonyhilyard.iceberg.compat.configured;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.anthonyhilyard.iceberg.config.IcebergConfigSpec;
import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class IcebergConfigProvider implements IConfigProvider
{
	@Override
	public Set<IModConfig> getConfigurationsForMod(ModContainer container)
	{
		// Add Iceberg configurations
		Set<IModConfig> configs = new HashSet<>();
		addIcebergConfigsToMap(container, ModConfig.Type.CLIENT, configs::add);
		addIcebergConfigsToMap(container, ModConfig.Type.COMMON, configs::add);
		addIcebergConfigsToMap(container, ModConfig.Type.SERVER, configs::add);
		return configs;
	}

	private static void addIcebergConfigsToMap(ModContainer container, ModConfig.Type type, Consumer<IModConfig> consumer)
	{
		getForgeTrackedConfigs().get(type).stream()
				.filter(config -> config.getModId().equals(container.getModId()) && config.getSpec() instanceof IcebergConfigSpec)
				.map(IcebergConfigPlugin::new)
				.collect(Collectors.toSet())
				.forEach(consumer);
	}

	private static EnumMap<ModConfig.Type, Set<ModConfig>> getForgeTrackedConfigs()
	{
		return ObfuscationReflectionHelper.getPrivateValue(ConfigTracker.class, ConfigTracker.INSTANCE, "configSets");
	}
	
}