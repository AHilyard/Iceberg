package com.anthonyhilyard.iceberg.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Selectors
{
	private static Map<String, Rarity> rarities = new HashMap<String, Rarity>() {{
		put("common", Rarity.COMMON);
		put("uncommon", Rarity.UNCOMMON);
		put("rare", Rarity.RARE);
		put("epic", Rarity.EPIC);
	}};

	private static Map<String, BiPredicate<Tag, String>> nbtComparators = new HashMap<String, BiPredicate<Tag, String>>() {{
		put("=",  (tag, value) -> {
			return tag.getAsString().contentEquals(value);
		});

		put("!=", (tag, value) -> !tag.getAsString().contentEquals(value));

		put(">",  (tag, value) -> {
			try
			{
				double parsedValue = Double.valueOf(value);
				if (tag instanceof NumericTag)
				{
					return ((NumericTag)tag).getAsDouble() > parsedValue;
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
				if (tag instanceof NumericTag)
				{
					return ((NumericTag)tag).getAsDouble() < parsedValue;
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

	public static record SelectorDocumentation(String name, String description, List<String> examples)
	{
		public SelectorDocumentation(String name, String description, String... examples) { this(name, description, Arrays.asList(examples)); }
	}

	public static List<SelectorDocumentation> selectorDocumentation()
	{
		return Arrays.asList(
			new SelectorDocumentation("Match all", "Specifying just an asterisk (*) will match all items.", "*"),
			new SelectorDocumentation("Item ID", "Use item ID to match single items.  Must include mod name for modded items.", "minecraft:stick", "iron_ore", "spoiledeggs:spoiled_egg"),
			new SelectorDocumentation("Tag", "$ followed by tag name to match all items with that tag.", "$forge:stone", "$planks"),
			new SelectorDocumentation("Mod name", "@ followed by mod identifier to match all items from that mod.", "@spoiledeggs"),
			new SelectorDocumentation("Rarity", "! followed by item's rarity to match all items with that rarity.  This is ONLY vanilla rarities.", "!uncommon", "!rare", "!epic"),
			new SelectorDocumentation("Item name color", "# followed by color hex code, to match all items with that exact color item name.", "#23F632"),
			new SelectorDocumentation("Display name", "% followed by any text.  Will match any item with this text (case-sensitive) in its tooltip display name.", "%Netherite", "%Uncommon"),
			new SelectorDocumentation("Tooltip text", "^ followed by any text.  Will match any item with this text (case-sensitive) anywhere in the tooltip text (besides the name).", "^Legendary"),
			new SelectorDocumentation("NBT tag", "& followed by tag name and optional comparator (=, >, <, or !=) and value, in the format <tag><comparator><value> or just <tag>.", "&Damage=0", "&Tier>1", "&map!=128", "&Enchantments"),
			new SelectorDocumentation("Negation", "~ followed by any selector above.  This selector will be negated, matching every item that does NOT match the selector.", "~minecraft:stick", "~!uncommon", "~@minecraft"),
			new SelectorDocumentation("Combining selectors", "Any number of selectors can be combined by separating them with a plus sign.", "minecraft:diamond_sword+&Enchantments", "minecraft:stick+~!common+&Damage=0")
		);
	}

	/**
	 * Returns true if this selector is syntactically valid.
	 * @param value The selector.
	 * @return True if the selector syntax is valid, false otherwise.
	 */
	public static boolean validateSelector(String value)
	{
		// First check if this is a combination of selectors.
		if (value.contains("+"))
		{
			for (String selector : value.split("\\+"))
			{
				if (!validateSelector(selector))
				{
					return false;
				}
			}
			return true;
		}

		// If this is a negation, remove the ~ and validate the rest.
		if (value.startsWith("~"))
		{
			return validateSelector(value.substring(1));
		}

		// If this is a wildcard selector, it is valid.
		if (value.contentEquals("*"))
		{
			return true;
		}

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
			return TextColor.parseColor(value) != null;
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
	@SuppressWarnings({"deprecation", "removal"})
	public static boolean itemMatches(ItemStack item, String selector)
	{
		// If this is a combination of selectors, check each one.
		if (selector.contains("+"))
		{
			for (String subSelector : selector.split("\\+"))
			{
				if (!itemMatches(item, subSelector))
				{
					return false;
				}
			}
			return true;
		}

		// If this is a negation, remove the ~ and check the rest.
		if (selector.startsWith("~"))
		{
			return !itemMatches(item, selector.substring(1));
		}
		
		// Wildcard
		if (selector.contentEquals("*"))
		{
			return true;
		}

		// Item ID
		String itemResourceLocation = ForgeRegistries.ITEMS.getKey(item.getItem()).toString();
		if (selector.equals(itemResourceLocation) || selector.equals(itemResourceLocation.replace("minecraft:", "")))
		{
			return true;
		}
		// Mod ID
		else if (selector.startsWith("@"))
		{
			if (itemResourceLocation.startsWith(selector.substring(1) + ":"))
			{
				return true;
			}
		}
		// Item name color
		else if (selector.startsWith("#"))
		{
			TextColor entryColor = TextColor.parseColor(selector);
			if (entryColor != null && entryColor.equals(ItemColor.getColorForItem(item, TextColor.fromRgb(0xFFFFFF))))
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
		// Item tag
		else if (selector.startsWith("$"))
		{
			Optional<TagKey<Item>> matchingTag = BuiltInRegistries.ITEM.getTagNames().filter(tagKey -> tagKey.location().equals(new ResourceLocation(selector.substring(1)))).findFirst();
			if (matchingTag.isPresent() && item.is(matchingTag.get()))
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
		// NBT tag
		else if (selector.startsWith("&"))
		{
			String tagName = selector.substring(1);
			String tagValue = null;
			BiPredicate<Tag, String> valueChecker = null;

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
	private static boolean findMatchingSubtag(Tag tag, String key, String value, BiPredicate<Tag, String> valueChecker)
	{
		if (tag == null)
		{
			return false;
		}

		if (tag.getId() == Tag.TAG_COMPOUND)
		{
			CompoundTag compoundTag = (CompoundTag)tag;

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
					if (compoundTag.getTagType(innerKey) == Tag.TAG_LIST || compoundTag.getTagType(innerKey) == Tag.TAG_COMPOUND)
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
		else if (tag.getId() == Tag.TAG_LIST)
		{
			ListTag listTag = (ListTag)tag;
			for (Tag innerTag : listTag)
			{
				if (innerTag.getId() == Tag.TAG_LIST || innerTag.getId() == Tag.TAG_COMPOUND)
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
