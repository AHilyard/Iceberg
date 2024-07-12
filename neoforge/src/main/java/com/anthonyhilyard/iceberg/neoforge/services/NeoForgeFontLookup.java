package com.anthonyhilyard.iceberg.neoforge.services;

import com.anthonyhilyard.iceberg.services.IFontLookup;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.FontContext;

public class NeoForgeFontLookup implements IFontLookup
{
	@Override
	public Font getTooltipFont(ItemStack itemStack, Screen screen)
	{
		return IClientItemExtensions.of(itemStack).getFont(itemStack, FontContext.TOOLTIP);
	}
}
