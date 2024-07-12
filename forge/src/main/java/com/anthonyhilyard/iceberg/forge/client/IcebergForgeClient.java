package com.anthonyhilyard.iceberg.forge.client;

import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.util.Tooltips.TitleBreakComponent;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class IcebergForgeClient
{
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void event(RegisterClientTooltipComponentFactoriesEvent event)
	{
		event.register(TitleBreakComponent.class, RegisterTooltipComponentFactoryEvent.EVENT.invoker()::getComponent);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void event(ItemTooltipEvent event)
	{
		Minecraft minecraft = Minecraft.getInstance();
		com.anthonyhilyard.iceberg.events.client.ItemTooltipEvent.EVENT.invoker().onItemTooltip(event.getItemStack(), TooltipContext.of(minecraft.level), event.getFlags(), event.getToolTip());
	}
}
