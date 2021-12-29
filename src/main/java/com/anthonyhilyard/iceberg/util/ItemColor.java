package com.anthonyhilyard.iceberg.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ICharacterConsumer;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

public class ItemColor
{
	private static class ColorCollector implements ICharacterConsumer
	{
		private Color color = null;

		@Override
		public boolean accept(int index, Style style, int codePoint)
		{
			if (style.getColor() != null)
			{
				color = style.getColor();
				return false;
			}
			return true;
		}

		public Color getColor() { return color; }
	}

	public static Color findFirstColorCode(ITextComponent textComponent)
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
					TextFormatting format = TextFormatting.getByCode(rawTitle.charAt(i + 1));
					if (format != null && format.isColor())
					{
						return Color.fromLegacyFormat(format);
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

		// If the item has a special hover name color (Stored in NBT), use that.
		if (!item.getHoverName().getStyle().isEmpty() && item.getHoverName().getStyle().getColor() != null)
		{
			result = item.getHoverName().getStyle().getColor();
		}

		// If there is a color code specified for the item name, use that.
		Color formattingColor = findFirstColorCode(item.getHoverName());
		if (formattingColor != null)
		{
			result = formattingColor;
		}

		// Finally, if there is a color style stored per-character, use the first one found.
		ColorCollector colorCollector = new ColorCollector();
		item.getHoverName().getVisualOrderText().accept(colorCollector);
		if (colorCollector.getColor() != null)
		{
			result = colorCollector.getColor();
		}

		// Fallback to the default color if we somehow haven't found a single valid color.
		if (result == null)
		{
			result = defaultColor;
		}

		return result;
	}
}
