package com.anthonyhilyard.iceberg.neoforge;

import java.util.Locale;
import java.util.Optional;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.client.IcebergClient;
import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.config.IcebergConfig;
import com.anthonyhilyard.iceberg.neoforge.client.IcebergNeoForgeClient;
import com.anthonyhilyard.iceberg.neoforge.config.NeoForgeIcebergConfigSpec;
import com.anthonyhilyard.iceberg.neoforge.server.IcebergNeoForgeServer;
import com.anthonyhilyard.iceberg.neoforge.services.NeoForgeKeyMappingRegistrar;
import com.anthonyhilyard.iceberg.neoforge.services.NeoForgeReloadListenerRegistrar;
import com.electronwill.nightconfig.core.Config;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Iceberg.MODID)
public final class IcebergNeoForge
{
	public IcebergNeoForge(IEventBus modBus)
	{
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			// Common setup.
			IcebergClient.init();

			NeoForge.EVENT_BUS.register(IcebergNeoForgeClient.NeoForgeEvents.class);
			modBus.register(IcebergNeoForgeClient.ModEvents.class);
			modBus.register(NeoForgeKeyMappingRegistrar.class);
			modBus.register(NeoForgeReloadListenerRegistrar.class);
		}
		else
		{
			NeoForge.EVENT_BUS.register(IcebergNeoForgeServer.class);
		}
	}

	@SuppressWarnings("unused")
	private static void registerConfig(Class<? extends IcebergConfig<?>> clazz, IIcebergConfigSpec spec, String modid)
	{
		// Get the mod container for the appropriate mod.
		Optional<? extends ModContainer> container = ModList.get().getModContainerById(modid);

		if (container.isPresent())
		{
			Config.setInsertionOrderPreserved(true);
			container.get().registerConfig(ModConfig.Type.COMMON, (NeoForgeIcebergConfigSpec)spec, String.format(Locale.ROOT, "%s.toml", modid));
		}
	}
}
