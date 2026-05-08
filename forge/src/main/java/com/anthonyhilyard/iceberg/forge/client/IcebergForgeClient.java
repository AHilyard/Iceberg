package com.anthonyhilyard.iceberg.forge.client;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class IcebergForgeClient
{
	/*
	@Mod.EventBusSubscriber(modid = Iceberg.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class ForgeEvents
	{
		@SubscribeEvent(priority = Priority.HIGH)
		public static void event(ItemTooltipEvent event)
		{
			Minecraft minecraft = Minecraft.getInstance();
			ItemTooltipEvent.EVENT.invoker().onItemTooltip(event.getItemStack(), TooltipContext.of(minecraft.level), event.getFlags(), event.getToolTip());
		}

		@SubscribeEvent(priority = Priority.HIGH)
		public static void tooltipPreRenderEvent(RenderTooltipEvent.Pre event)
		{
			PreExtResult result = RenderTooltipEvents.PREEXT.invoker().onPre(event.getItemStack(), event.getGraphics(), event.getX(), event.getY(), event.getScreenWidth(), event.getScreenHeight(), event.getFont(), event.getComponents(), event.getTooltipPositioner(), false, 0);
			event.setFont(result.font());
			event.setX(result.x());
			event.setY(result.y());
			event.setCanceled(result.result() != InteractionResult.PASS);
		}

		@SubscribeEvent(priority = Priority.HIGH)
		public static void tooltipGatherEvent(RenderTooltipEvent.GatherComponents event)
		{
			GatherResult result = RenderTooltipEvents.GATHER.invoker().onGather(event.getItemStack(), event.getScreenWidth(), event.getScreenHeight(), event.getTooltipElements(), event.getMaxWidth(), 0);
			event.setMaxWidth(result.maxWidth());
			event.setCanceled(result.result() != InteractionResult.PASS);
		}
	}
	*/

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
