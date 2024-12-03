package com.anthonyhilyard.iceberg.util;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public class ItemUtil
{
	public static DataComponentMap getItemComponents(final ItemStack item)
	{
		return !item.getComponents().isEmpty() ? new PatchedDataComponentMap(item.getComponents()) : DataComponentMap.EMPTY;
	}

	public static EquipmentSlot getEquipmentSlot(ItemStack itemStack)
	{
		Equippable equippable = (Equippable)itemStack.get(DataComponents.EQUIPPABLE);
		if (equippable != null)
		{
			return equippable.slot();
		}

		return EquipmentSlot.MAINHAND;
	}
}
