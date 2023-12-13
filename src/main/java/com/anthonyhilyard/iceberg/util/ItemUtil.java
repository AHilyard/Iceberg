package com.anthonyhilyard.iceberg.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class ItemUtil
{
	public static CompoundTag getItemNBT(final ItemStack item)
	{
		return item.hasTag() ? item.getTag().copy() : null;
	}
}
