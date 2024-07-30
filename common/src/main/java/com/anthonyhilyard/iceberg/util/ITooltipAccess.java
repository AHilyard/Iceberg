package com.anthonyhilyard.iceberg.util;

import net.minecraft.world.item.ItemStack;

public interface ITooltipAccess
{
	void setIcebergTooltipStack(ItemStack stack);
	ItemStack getIcebergTooltipStack();
}
