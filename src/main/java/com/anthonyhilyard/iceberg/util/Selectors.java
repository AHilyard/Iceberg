package com.anthonyhilyard.iceberg.util;

import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NumberNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Color;
import net.minecraft.util.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.item.Rarity;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraftforge.common.util.Constants.NBT;

public class Selectors
{
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

	public static class SelectorDocumentation
	{
		public final String name;
		public final String description;
		public final List<String> examples;

		public SelectorDocumentation(String name, String description, String... examples)
		{
			this.name = name;
			this.description = description;
			this.examples = Arrays.asList(examples);
		}
	}

	public static List<SelectorDocumentation> selectorDocumentation()
	{
		return Arrays.asList(
			new SelectorDocumentation("Item name", "Use item name for vanilla items or include mod name for modded items.", "minecraft:stick", "iron_ore"),
			new SelectorDocumentation("Tag", "$ followed by tag name.", "$forge:stone", "$planks"),
			new SelectorDocumentation("Mod name", "@ followed by mod identifier.", "@spoiledeggs"),
			new SelectorDocumentation("Rarity", "! followed by item's rarity.  This is ONLY vanilla rarities.", "!uncommon", "!rare", "!epic"),
			new SelectorDocumentation("Item name color", "# followed by color hex code, the hex code must match exactly.", "#23F632"),
			new SelectorDocumentation("Display name", "% followed by any text.  Will match any item with this text in its tooltip display name.", "%Netherite", "%[Uncommon]"),
			new SelectorDocumentation("Tooltip text", "Will match any item with this text anywhere in the tooltip text (besides the name).", "^Legendary"),
			new SelectorDocumentation("NBT tag", "& followed by tag name and optional comparator (=, >, <, or !=) and value, in the format <tag><comparator><value> or just <tag>.", "&Damage=0", "&Tier>1", "&map!=128", "&Enchantments")
		);
	}

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

			// Look for a tag matching the given name and value.
			return findMatchingSubtag(item.getTag(), tagName, tagValue, valueChecker);
		}

		return false;
	}

	/**
	 * Retrieves the first inner tag with the given key, or null if there is no match.
	 */
	private static boolean findMatchingSubtag(INBT tag, String key, String value, BiPredicate<INBT, String> valueChecker)
	{
		if (tag == null)
		{
			return false;
		}

		if (tag.getId() == NBT.TAG_COMPOUND)
		{
			CompoundNBT compoundTag = (CompoundNBT)tag;

			if (compoundTag.contains(key))
			{
				// Just checking presence.
				if (value == null && valueChecker == null)
				{
					return true;
				}
				// Otherwise, we will use the provided comparator.
				else
				{
					return valueChecker.test(compoundTag.get(key), value);
				}
			}
			else
			{
				for (String innerKey : compoundTag.getAllKeys())
				{
					if (compoundTag.getTagType(innerKey) == NBT.TAG_LIST || compoundTag.getTagType(innerKey) == NBT.TAG_COMPOUND)
					{
						if (findMatchingSubtag(compoundTag.get(innerKey), key, value, valueChecker))
						{
							return true;
						}
					}
				}
				return false;
			}
		}
		else if (tag.getId() == NBT.TAG_LIST)
		{
			ListNBT listTag = (ListNBT)tag;
			for (INBT innerTag : listTag)
			{
				if (innerTag.getId() == NBT.TAG_LIST || innerTag.getId() == NBT.TAG_COMPOUND)
				{
					if (findMatchingSubtag(innerTag, key, value, valueChecker))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
}