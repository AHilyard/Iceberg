package com.anthonyhilyard.iceberg.neoforge.client;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

public class IcebergNeoForgeClient
{
	/*
	@EventBusSubscriber(modid = Iceberg.MODID, value = Dist.CLIENT)
	public static class NeoForgeEvents
	{
		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void itemTooltipEvent(ItemTooltipEvent event)
		{
			Minecraft minecraft = Minecraft.getInstance();
			ItemTooltipEvent.EVENT.invoker().onItemTooltip(event.getItemStack(), TooltipContext.of(minecraft.level), event.getFlags(), event.getToolTip());
		}

		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void tooltipPreRenderEvent(RenderTooltipEvent.Pre event)
		{
			PreExtResult result = RenderTooltipEvents.PREEXT.invoker().onPre(event.getItemStack(), event.getGraphics(), event.getX(), event.getY(), event.getScreenWidth(), event.getScreenHeight(), event.getFont(), event.getComponents(), event.getTooltipPositioner(), false, 0);
			event.setFont(result.font());
			event.setX(result.x());
			event.setY(result.y());
			event.setCanceled(result.result() != InteractionResult.PASS);
		}

		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void tooltipGatherEvent(RenderTooltipEvent.GatherComponents event)
		{
			GatherResult result = RenderTooltipEvents.GATHER.invoker().onGather(event.getItemStack(), event.getScreenWidth(), event.getScreenHeight(), event.getTooltipElements(), event.getMaxWidth(), 0);
			event.setMaxWidth(result.maxWidth());
			event.setCanceled(result.result() != InteractionResult.PASS);
		}
	}
	 */

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
