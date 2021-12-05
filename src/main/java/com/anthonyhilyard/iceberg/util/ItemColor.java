package com.anthonyhilyard.iceberg.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class ItemColor
{
	public static TextColor findFirstColorCode(Component textComponent)
	{
		// This function finds the first specified color code in the given text component.
		// It is intended to skip non-color formatting codes.
		String rawTitle = textComponent.getString();
		for (int i = 0; i < rawTitle.length(); i += 2)
		{
			// If we encounter a formatting code, check to see if it's a color.  If so, return it.
			if (rawTitle.charAt(i) == '\u00a7')
			{
				try
				{
					ChatFormatting format = ChatFormatting.getByCode(rawTitle.charAt(i + 1));
					if (format.isColor())
					{
						return TextColor.fromLegacyFormat(format);
					}
				}
				catch (StringIndexOutOfBoundsException e)
				{
					return null;
				}
			}
			// Otherwise, if we encounter a non-formatting character, bail.
			else
			{
				return null;
			}
		}
		return null;
	}

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

		// If the item has a special hover name TextColor (Stored in NBT), use that.
		if (!item.getHoverName().getStyle().isEmpty() && item.getHoverName().getStyle().getColor() != null)
		{
			result = item.getHoverName().getStyle().getColor();
		}

		// Finally if there is a color code specified for the item name, use that.
		TextColor formattingColor = findFirstColorCode(item.getItem().getName(item));
		if (formattingColor != null)
		{
			result = formattingColor;
		}

		// Fallback to the default TextColor if we somehow haven't found a single valid TextColor.
		if (result == null)
		{
			result = defaultColor;
		}

		return result;
	}
}
