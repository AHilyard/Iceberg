package com.anthonyhilyard.iceberg.neoforge;

import java.util.Locale;
import java.util.Optional;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.client.IcebergClient;
import com.anthonyhilyard.iceberg.config.IIcebergConfigSpec;
import com.anthonyhilyard.iceberg.config.IcebergConfig;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;
import com.anthonyhilyard.iceberg.neoforge.config.NeoForgeIcebergConfigSpec;
import com.electronwill.nightconfig.core.Config;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Iceberg.MODID)
public final class IcebergNeoForge
{
	public IcebergNeoForge(IEventBus modBus)
	{
		// Common environment-agnostic setup.

		if (FMLEnvironment.getDist() == Dist.CLIENT)
		{
			// Client loader-agnostic setup.
			IcebergClient.init();
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

			ModContainer targetContainer = container.get();
			targetContainer.registerConfig(ModConfig.Type.COMMON, (NeoForgeIcebergConfigSpec)spec, String.format(Locale.ROOT, "%s.toml", modid));

			IEventBus targetBus = targetContainer.getEventBus();

			if (targetBus != null)
			{
				targetBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading event) -> {
					ConfigEvents.LOAD.invoker().onLoad(event.getConfig().getModId());
				});

				targetBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Reloading event) -> {
					ConfigEvents.RELOAD.invoker().onReload(event.getConfig().getModId());
				});
			}
		}
	}
}
