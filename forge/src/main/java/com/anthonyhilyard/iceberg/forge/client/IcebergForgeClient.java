package com.anthonyhilyard.iceberg.forge.client;

import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.PreExtResult;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class IcebergForgeClient
{
	public static class ForgeEvents
	{
		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void event(ItemTooltipEvent event)
		{
			Minecraft minecraft = Minecraft.getInstance();
			com.anthonyhilyard.iceberg.events.client.ItemTooltipEvent.EVENT.invoker().onItemTooltip(event.getItemStack(), TooltipContext.of(minecraft.level), event.getFlags(), event.getToolTip());
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
		public static void tooltipColorEvent(RenderTooltipEvent.Color event)
		{
			ColorExtResult result = RenderTooltipEvents.COLOREXT.invoker().onColor(event.getItemStack(), event.getGraphics(), event.getX(), event.getY(), event.getFont(), event.getBackgroundStart(), event.getBackgroundEnd(), event.getBorderStart(), event.getBorderEnd(), event.getComponents(), false, 0);
			event.setBackgroundStart(result.backgroundStart());
			event.setBackgroundEnd(result.backgroundEnd());
			event.setBorderStart(result.borderStart());
			event.setBorderEnd(result.borderEnd());
		}

		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void tooltipGatherEvent(RenderTooltipEvent.GatherComponents event)
		{
			GatherResult result = RenderTooltipEvents.GATHER.invoker().onGather(event.getItemStack(), event.getScreenWidth(), event.getScreenHeight(), event.getTooltipElements(), event.getMaxWidth(), 0);
			event.setMaxWidth(result.maxWidth());
			event.setCanceled(result.result() != InteractionResult.PASS);
		}
	}

	public static class ModEvents
	{
		@SubscribeEvent(priority = EventPriority.HIGH)
		public static void registerTooltipComponentsEvent(RegisterClientTooltipComponentFactoriesEvent event)
		{
			for (Class<? extends TooltipComponent> type : RegisterTooltipComponentFactoryEvent.EVENT.getListenerTypes().keySet())
			{
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
