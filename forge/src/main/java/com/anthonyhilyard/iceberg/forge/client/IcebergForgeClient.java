package com.anthonyhilyard.iceberg.forge.client;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class IcebergForgeClient
{
	@Mod.EventBusSubscriber(modid = Iceberg.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class ForgeEvents
	{
		@SubscribeEvent(priority = Priority.HIGH)
		public static void event(ItemTooltipEvent event)
		{
			Minecraft minecraft = Minecraft.getInstance();
			com.anthonyhilyard.iceberg.events.client.ItemTooltipEvent.EVENT.invoker().onItemTooltip(event.getItemStack(), Item.TooltipContext.of(minecraft.level), event.getFlags(), event.getToolTip());
		}

		@SubscribeEvent(priority = Priority.HIGH)
		public static void onGatherComponents(RenderTooltipEvent.GatherComponents event)
		{
			Minecraft mc = Minecraft.getInstance();
			int width = mc.getWindow().getGuiScaledWidth();
			int height = mc.getWindow().getGuiScaledHeight();

			RenderTooltipEvents.GATHER.invoker().onGather(event.getItemStack(), width, height, event.getTooltipElements(), event.getMaxWidth(), -1);
		}
	}
	@Mod.EventBusSubscriber(modid = Iceberg.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class ModEvents
	{
		@SubscribeEvent(priority = Priority.HIGH)
		public static void registerTooltipComponentsEvent(RegisterClientTooltipComponentFactoriesEvent event)
		{
			for (Class<? extends TooltipComponent> type : RegisterTooltipComponentFactoryEvent.EVENT.getListenerTypes().keySet())
			{
				event.register(type, RegisterTooltipComponentFactoryEvent.EVENT.invoker()::getComponent);
			}
		}

		@SubscribeEvent(priority = Priority.HIGH)
		public static void configLoadEvent(ModConfigEvent.Loading event)
		{
			ConfigEvents.LOAD.invoker().onLoad(event.getConfig().getModId());
		}

		@SubscribeEvent(priority = Priority.HIGH)
		public static void configReloadEvent(ModConfigEvent.Reloading event)
		{
			ConfigEvents.RELOAD.invoker().onReload(event.getConfig().getModId());
		}
	}
}
