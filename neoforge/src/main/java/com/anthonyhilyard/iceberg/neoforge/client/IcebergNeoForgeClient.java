package com.anthonyhilyard.iceberg.neoforge.client;

import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;
import com.anthonyhilyard.iceberg.util.Tooltips.TitleBreakComponent;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item.TooltipContext;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;


public class IcebergNeoForgeClient
{
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void registerTooltipComponentsEvent(RegisterClientTooltipComponentFactoriesEvent event)
	{
		event.register(TitleBreakComponent.class, RegisterTooltipComponentFactoryEvent.EVENT.invoker()::getComponent);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void event(ItemTooltipEvent event)
	{
		Minecraft minecraft = Minecraft.getInstance();
		com.anthonyhilyard.iceberg.events.client.ItemTooltipEvent.EVENT.invoker().onItemTooltip(event.getItemStack(), TooltipContext.of(minecraft.level), event.getFlags(), event.getToolTip());
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
