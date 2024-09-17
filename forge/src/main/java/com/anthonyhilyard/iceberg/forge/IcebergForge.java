package com.anthonyhilyard.iceberg.forge;

import java.util.Locale;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.client.IcebergClient;
import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.config.IcebergConfig;
import com.anthonyhilyard.iceberg.forge.client.IcebergForgeClient;
import com.anthonyhilyard.iceberg.forge.config.ForgeIcebergConfigSpec;
import com.anthonyhilyard.iceberg.forge.server.IcebergForgeServer;
import com.anthonyhilyard.iceberg.forge.services.ForgeKeyMappingRegistrar;
import com.anthonyhilyard.iceberg.forge.services.ForgeReloadListenerRegistrar;
import com.electronwill.nightconfig.core.Config;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Iceberg.MODID)
public final class IcebergForge
{
	private static FMLJavaModLoadingContext context;
	public IcebergForge(FMLJavaModLoadingContext context)
	{
		IcebergForge.context = context;
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			IEventBus modBus = context.getModEventBus();

			// Common setup.
			IcebergClient.init();

			MinecraftForge.EVENT_BUS.register(IcebergForgeClient.ForgeEvents.class);
			modBus.register(IcebergForgeClient.ModEvents.class);
			modBus.register(ForgeKeyMappingRegistrar.class);
			modBus.register(ForgeReloadListenerRegistrar.class);
		}
		else
		{
			MinecraftForge.EVENT_BUS.register(IcebergForgeServer.class);
		}
	}

	@SuppressWarnings("unused")
	private static void registerConfig(Class<? extends IcebergConfig<?>> clazz, IIcebergConfigSpec spec, String modid)
	{
		Config.setInsertionOrderPreserved(true);

		ForgeIcebergConfigSpec forgeSpec = (ForgeIcebergConfigSpec)spec;

		if (forgeSpec.isEmpty())
		{
			// This handles the case where a mod tries to register a config, without any options configured inside it.
			Iceberg.LOGGER.debug("Attempted to register an empty config on mod {}", modid);
			return;
		}

		ModContainer container = context.getContainer();
		container.addConfig(new ModConfig(ModConfig.Type.COMMON, forgeSpec, container, String.format(Locale.ROOT, "%s.toml", modid)));
	}
}
