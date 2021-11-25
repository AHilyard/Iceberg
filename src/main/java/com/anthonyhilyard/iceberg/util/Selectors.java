package com.anthonyhilyard.iceberg.util;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NumberNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Color;
import net.minecraft.util.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.item.Rarity;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;

public class Selectors
{
	private static final int TAG_COMPOUND = 10;

	private static Map<String, Rarity> rarities = new HashMap<String, Rarity>() {{
		put("common", Rarity.COMMON);
		put("uncommon", Rarity.UNCOMMON);
		put("rare", Rarity.RARE);
		put("epic", Rarity.EPIC);
	}};

	private static Map<String, BiPredicate<INBT, String>> nbtComparators = new HashMap<String, BiPredicate<INBT, String>>() {{

		put("=",  (tag, value) -> tag.getAsString().contentEquals(value));

		put("!=", (tag, value) -> !tag.getAsString().contentEquals(value));

		put(">",  (tag, value) -> {
			try
			{
				double parsedValue = Double.valueOf(value);
				if (tag instanceof NumberNBT)
				{
					return ((NumberNBT)tag).getAsDouble() > parsedValue;
				}
				else
				{
					return false;
				}
			}
			catch (Exception e)
			{
				return false;
			}
		});

		put("<",  (tag, value) -> {
			try
			{
				double parsedValue = Double.valueOf(value);
				if (tag instanceof NumberNBT)
				{
					return ((NumberNBT)tag).getAsDouble() < parsedValue;
				}
				else
				{
					return false;
				}
			}
			catch (Exception e)
			{
				return false;
			}
		});
	}};

	/**
	 * Returns true if this selector is syntactically valid.
	 * @param value The selector.
	 * @return True if the selector syntax is valid, false otherwise.
	 */
	public static boolean validateSelector(String value)
	{
		// This is a tag, which should be a resource location.
		if (value.startsWith("$"))
		{
			return ResourceLocation.isValidResourceLocation(value.substring(1));
		}
		// Mod IDs need to conform to this regex: ^[a-z][a-z0-9_-]{1,63}$
		else if (value.startsWith("@"))
		{
			return value.substring(1).matches("^[a-z][a-z0-9_-]{1,63}$");
		}
		// If this is a rarity, make sure it's a valid one.
		else if (value.startsWith("!"))
		{
			return rarities.keySet().contains(value.substring(1).toLowerCase());
		}
		// If this is a hex color, ensure it's in a valid format.
		else if (value.startsWith("#"))
		{
			return Color.parseColor(value) != null;
		}
		// Text matches are always considered valid.
		else if (value.startsWith("%") || value.startsWith("^"))
		{
			return true;
		}
		// Any text is valid for NBT tag selectors.
		else if (value.startsWith("&"))
		{
			return true;
		}
		// Otherwise it's an item, so just make sure it's a value resource location.
		else
		{
			return value == null || value == "" || ResourceLocation.isValidResourceLocation(value);
		}
	}

	/**
	 * Returns true if the given item is matched by the given selector.
	 * @param item An ItemStack instance of an item to check.
	 * @param selector A selector string to check against.
	 * @return True if the item matches, false otherwise.
	 */
	public static boolean itemMatches(ItemStack item, String selector)
	{
		String itemResourceLocation = item.getItem().getRegistryName().toString();
		// Item ID
		if (selector.equals(itemResourceLocation) || selector.equals(itemResourceLocation.replace("minecraft:", "")))
		{
			return true;
		}
		// Item name color
		else if (selector.startsWith("#"))
		{
			Color entryColor = Color.parseColor(selector);
			if (entryColor != null && entryColor.equals(ItemColor.getColorForItem(item, Color.fromRgb(0xFFFFFF))))
			{
				return true;
			}
		}
		// Vanilla rarity
		else if (selector.startsWith("!"))
		{
			if (item.getRarity() == rarities.get(selector.substring(1)))
			{
				return true;
			}
		}
		// Mod ID
		else if (selector.startsWith("@"))
		{
			if (itemResourceLocation.startsWith(selector.substring(1) + ":"))
			{
				return true;
			}
		}
		// Item tag
		else if (selector.startsWith("$"))
		{
			if (ItemTags.getAllTags().getTagOrEmpty(new ResourceLocation(selector.substring(1))).getValues().contains(item.getItem()))
			{
				return true;
			}
		}
		// Item display name
		else if (selector.startsWith("%"))
		{
			if (item.getDisplayName().getString().contains(selector.substring(1)))
			{
				return true;
			}
		}
		// Tooltip text
		else if (selector.startsWith("^"))
		{
			Minecraft mc = Minecraft.getInstance();
			List<ITextComponent> lines = item.getTooltipLines(mc.player, TooltipFlags.ADVANCED);
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
		// NBT tag
		else if (selector.startsWith("&"))
		{
			String tagName = selector.substring(1);
			String tagValue = null;
			BiPredicate<INBT, String> valueChecker = null;

			// This implementation means tag names containing and comparator strings can't be compared.
			// Hopefully this isn't common.
			for (String comparator : nbtComparators.keySet())
			{
				if (tagName.contains(comparator))
				{
					valueChecker = nbtComparators.get(comparator);
					String[] components = tagName.split(comparator);
					tagName = components[0];
					if (components.length > 1)
					{
						tagValue = components[1];
					}
					break;
				}
			}

			// Look for a tag matching the given name.
			INBT matchedTag = getSubtag(item.getTag(), tagName);
			if (matchedTag != null)
			{
				// A tag value of null means that we are just looking for the presence of this tag.
				if (tagValue == null)
				{
					return true;
				}
				// Otherwise, we will use the provided comparator.
				else
				{
					if (valueChecker != null)
					{
						return valueChecker.test(matchedTag, tagValue);
					}
				}
			}
		}

		return false;
	}

	/**
	 * Retrieves the first inner tag with the given key, or null if there is no match.
	 */
	private static INBT getSubtag(CompoundNBT tag, String key)
	{
		if (tag == null)
		{
			return null;
		}

		if (tag.contains(key))
		{
			return tag.get(key);
		}
		else
		{
			for (String innerKey : tag.getAllKeys())
			{
				if (tag.getTagType(innerKey) == TAG_COMPOUND)
				{
					INBT innerTag = getSubtag(tag.getCompound(innerKey), key);
					if (innerTag != null)
					{
						return innerTag;
					}
				}
			}
			return null;
		}
	}
}