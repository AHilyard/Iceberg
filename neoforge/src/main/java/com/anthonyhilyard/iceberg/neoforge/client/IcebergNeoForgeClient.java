package com.anthonyhilyard.iceberg.neoforge.client;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class IcebergNeoForgeClient
{
	@EventBusSubscriber(modid = Iceberg.MODID, value = Dist.CLIENT)
	public static class NeoForgeEvents
	{
		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void itemTooltipEvent(ItemTooltipEvent event)
		{
			Minecraft minecraft = Minecraft.getInstance();
			com.anthonyhilyard.iceberg.events.client.ItemTooltipEvent.EVENT.invoker().onItemTooltip(event.getItemStack(), Item.TooltipContext.of(minecraft.level), event.getFlags(), event.getToolTip());
		}
	}
	@EventBusSubscriber(modid = Iceberg.MODID, value = Dist.CLIENT)
	public static class ModEvents
	{
		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void registerTooltipComponentsEvent(RegisterClientTooltipComponentFactoriesEvent event)
		{
			for (Class<? extends TooltipComponent> type : RegisterTooltipComponentFactoryEvent.EVENT.getListenerTypes().keySet())
			{
				Iceberg.LOGGER.debug("Registering tooltip component: " + type.getName());
				event.register(type, RegisterTooltipComponentFactoryEvent.EVENT.invoker()::getComponent);
			}
		}

		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void configLoadEvent(ModConfigEvent.Loading event)
		{
			ConfigEvents.LOAD.invoker().onLoad(event.getConfig().getModId());
		}

		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void configReloadEvent(ModConfigEvent.Reloading event)
		{
			ConfigEvents.RELOAD.invoker().onReload(event.getConfig().getModId());
		}
	}
}
