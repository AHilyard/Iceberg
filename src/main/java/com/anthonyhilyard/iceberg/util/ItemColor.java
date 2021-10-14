package com.anthonyhilyard.iceberg.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.TextColor;

public class ItemColor
{
	public static TextColor getColorForItem(ItemStack item, TextColor defaultColor)
	{
		TextColor result = null;

		// TextColor based on rarity value.
		result = item.getDisplayName().getStyle().getColor();

		// Some mods override the getName() method of the Item class, so grab that TextColor if it's there.
		if (item.getItem() != null &&
			item.getItem().getName(item) != null &&
			item.getItem().getName(item).getStyle() != null &&
			item.getItem().getName(item).getStyle().getColor() != null)
		{
			result = item.getItem().getName(item).getStyle().getColor();
		}

		// Finally, if the item has a special hover name TextColor (Stored in NBT), use that.
		if (!item.getHoverName().getStyle().isEmpty() && item.getHoverName().getStyle().getColor() != null)
		{
			result = item.getHoverName().getStyle().getColor();
		}

		// Fallback to the default TextColor if we somehow haven't found a single valid TextColor.
		if (result == null)
		{
			result = defaultColor;
		}

		return result;
	}
}
