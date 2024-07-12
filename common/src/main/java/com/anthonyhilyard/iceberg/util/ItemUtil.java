package com.anthonyhilyard.iceberg.util;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

public class ItemUtil
{
	public static DataComponentMap getItemComponents(final ItemStack item)
	{
		return !item.getComponents().isEmpty() ? new PatchedDataComponentMap(item.getComponents()) : DataComponentMap.EMPTY;
	}

	public static EquipmentSlot getEquipmentSlot(ItemStack itemStack)
	{
		Equipable equipable = Equipable.get(itemStack);
		if (equipable != null)
		{
			return equipable.getEquipmentSlot();
		}

		return EquipmentSlot.MAINHAND;
	}
}
