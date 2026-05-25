package com.anthonyhilyard.iceberg.util;

import net.minecraft.world.item.ItemStack;

public interface INestedTooltipAccess
{
	void setIcebergNestedTooltipStack(ItemStack stack);
	ItemStack getIcebergNestedTooltipStack();
}
