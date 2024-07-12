package com.anthonyhilyard.iceberg.fabric.services;

import com.anthonyhilyard.iceberg.services.IFontLookup;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class FabricFontLookup implements IFontLookup
{
	@Override
	public Font getTooltipFont(ItemStack itemStack, Screen screen)
	{
		return Screens.getTextRenderer(screen);
	}
}
