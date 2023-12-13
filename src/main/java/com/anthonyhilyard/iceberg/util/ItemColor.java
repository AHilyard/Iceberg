package com.anthonyhilyard.iceberg.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

@Deprecated(forRemoval = true)
public class ItemColor
{
	private static class ColorCollector implements FormattedCharSink
	{
		private TextColor color = null;

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

		public TextColor getColor() { return color; }
	}

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
					if (format != null && format.isColor())
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

		// If there is a color code specified for the item name, use that.
		TextColor formattingColor = findFirstColorCode(item.getHoverName());
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

		// If we haven't found a color or we're still using the rarity color, check the actual tooltip.
		// This is slow, so it better get cached externally!
		if (result == null || result.equals(item.getDisplayName().getStyle().getColor()))
		{
			Minecraft mc = Minecraft.getInstance();
			List<Component> lines = null;

			try
			{
				lines = item.getTooltipLines(mc.player, TooltipFlag.Default.ADVANCED);
			}
			catch (Exception e)
			{
				// An item must have misbehaved.
			}

			if (lines != null && !lines.isEmpty())
			{
				result = lines.get(0).getStyle().getColor();
			}
		}

		// Fallback to the default TextColor if we somehow haven't found a single valid TextColor.
		if (result == null)
		{
			result = defaultColor;
		}

		return result;
	}
}
