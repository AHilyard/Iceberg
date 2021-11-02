package com.anthonyhilyard.iceberg.util;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

public class Selectors
{
	private static Map<String, Rarity> rarities = new HashMap<String, Rarity>() {{
		put("common", Rarity.COMMON);
		put("uncommon", Rarity.UNCOMMON);
		put("rare", Rarity.RARE);
		put("epic", Rarity.EPIC);
	}};

	public static boolean itemMatches(ItemStack item, String selector)
	{
		String itemResourceLocation = item.getItem().getRegistryName().toString();
		if (selector.equals(itemResourceLocation) || selector.equals(itemResourceLocation.replace("minecraft:", "")))
		{
			return true;
		}
		else if (selector.startsWith("#"))
		{
			TextColor entryColor = TextColor.parseColor(selector);
			if (entryColor != null && entryColor.equals(ItemColor.getColorForItem(item, TextColor.fromRgb(0xFFFFFF))))
			{
				return true;
			}
		}
		else if (selector.startsWith("!"))
		{
			if (item.getRarity() == rarities.get(selector.substring(1)))
			{
				return true;
			}
		}
		else if (selector.startsWith("@"))
		{
			if (itemResourceLocation.startsWith(selector.substring(1) + ":"))
			{
				return true;
			}
		}
		else if (selector.startsWith("$"))
		{
			if (ItemTags.getAllTags().getTagOrEmpty(new ResourceLocation(selector.substring(1))).getValues().contains(item.getItem()))
			{
				return true;
			}
		}
		else if (selector.startsWith("%"))
		{
			if (item.getDisplayName().getString().contains(selector.substring(1)))
			{
				return true;
			}
		}
		else if (selector.startsWith("^"))
		{
			Minecraft mc = Minecraft.getInstance();
			List<Component> lines = item.getTooltipLines(mc.player, TooltipFlag.Default.ADVANCED);
			String tooltipText = "";

			// Skip title line.
			for (int n = 1; n < lines.size(); n++)
			{
				tooltipText += lines.get(n).getString() + '\n';
			}
			if (tooltipText.contains(selector.substring(1)))
			{
				return true;
			}
		}

		return false;
	}
}