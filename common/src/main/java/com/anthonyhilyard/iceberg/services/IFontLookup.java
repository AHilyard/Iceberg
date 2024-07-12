package com.anthonyhilyard.iceberg.services;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public interface IFontLookup
{
	Font getTooltipFont(ItemStack itemStack, Screen screen);
}
