package com.anthonyhilyard.iceberg.fabric.client;

import com.anthonyhilyard.iceberg.client.IcebergClient;
import com.anthonyhilyard.iceberg.events.client.ItemTooltipEvent;
import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.fabric.config.ConfigTracker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.loader.api.FabricLoader;

public final class IcebergFabricClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		// Common setup.
		IcebergClient.init();

		// Register all client events.
		TooltipComponentCallback.EVENT.register(RegisterTooltipComponentFactoryEvent.EVENT.invoker()::getComponent);
		ItemTooltipCallback.EVENT.register(ItemTooltipEvent.EVENT.invoker()::onItemTooltip);

		// Load configs.
		ConfigTracker.INSTANCE.loadConfigs(FabricLoader.getInstance().getConfigDir());
	}
}
