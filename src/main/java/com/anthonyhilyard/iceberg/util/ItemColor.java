package com.anthonyhilyard.iceberg.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Color;

public class ItemColor
{
	public static Color getColorForItem(ItemStack item, Color defaultColor)
	{
		Color result = null;

		// Color based on rarity value.
		result = item.getDisplayName().getStyle().getColor();

		// Some mods override the getName() method of the Item class, so grab that color if it's there.
		if (item.getItem() != null &&
			item.getItem().getName(item) != null &&
			item.getItem().getName(item).getStyle() != null &&
			item.getItem().getName(item).getStyle().getColor() != null)
		{
			result = item.getItem().getName(item).getStyle().getColor();
		}

		// Finally, if the item has a special hover name color (Stored in NBT), use that.
		if (!item.getHoverName().getStyle().isEmpty() && item.getHoverName().getStyle().getColor() != null)
		{
			result = item.getHoverName().getStyle().getColor();
		}

		// Fallback to the default color if we somehow haven't found a single valid color.
		if (result == null)
		{
			result = defaultColor;
		}

		return result;
	}
}
