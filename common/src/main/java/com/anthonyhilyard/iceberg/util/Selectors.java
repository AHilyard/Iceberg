package com.anthonyhilyard.iceberg.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
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
			new SelectorDocumentation("NBT/Item component", "& followed by tag or component name and optional comparator (=, >, <, or !=) and value, in the format <name><comparator><value> or just <name>.", "&damage>100", "&Tier>1", "&map_id!=128", "&enchantments"),
			new SelectorDocumentation("Negation", "~ followed by any selector above.  This selector will be negated, matching every item that does NOT match the selector.", "~minecraft:stick", "~!uncommon", "~@minecraft"),
			new SelectorDocumentation("Combining selectors", "Any number of selectors can be combined by separating them with a plus sign.", "minecraft:diamond_sword+&enchantments", "minecraft:stick+~!common+&damage>100")
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
			return ResourceLocation.tryParse(value.substring(1)) != null;
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
			return TextColor.parseColor(value).result().orElse(null) != null;
		}
		// Text matches are always considered valid.
		else if (value.startsWith("%") || value.startsWith("^"))
		{
			return true;
		}
		// Any text is valid for NBT/component selectors.
		else if (value.startsWith("&"))
		{
			return true;
		}
		// Otherwise it's an item, so just make sure it's a value resource location.
		else
		{
			return value == null || value == "" || ResourceLocation.tryParse(value) != null;
		}
	}

	/**
	 * Returns true if the given item is matched by the given selector.
	 * @param item An ItemStack instance of an item to check.
	 * @param selector A selector string to check against.
	 * @return True if the item matches, false otherwise.
	 */
	@SuppressWarnings("removal")
	public static boolean itemMatches(ItemStack item, String selector, HolderLookup.Provider provider)
	{
		if (item.isEmpty())
		{
			return false;
		}

		// If this is a combination of selectors, check each one.
		if (selector.contains("+"))
		{
			for (String subSelector : selector.split("\\+"))
			{
				if (!itemMatches(item, subSelector, provider))
				{
					return false;
				}
			}
			return true;
		}

		// If this is a negation, remove the ~ and check the rest.
		if (selector.startsWith("~"))
		{
			return !itemMatches(item, selector.substring(1), provider);
		}
		
		// Wildcard
		if (selector.contentEquals("*"))
		{
			return true;
		}

		// Item ID
		String itemResourceLocation = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
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
			TextColor entryColor = TextColor.parseColor(selector).result().orElse(null);
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
			Optional<TagKey<Item>> matchingTag = BuiltInRegistries.ITEM.getTagNames().filter(tagKey -> tagKey.location().equals(ResourceLocation.parse(selector.substring(1)))).findFirst();
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
			List<Component> lines = item.getTooltipLines(TooltipContext.EMPTY, mc.player, TooltipFlag.Default.ADVANCED);
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
		// NBT tag/item component
		else if (selector.startsWith("&"))
		{
			String name = selector.substring(1);
			String value = null;
			BiPredicate<Tag, String> valueChecker = null;

			// This implementation means tag names containing and comparator strings can't be compared.
			// Hopefully this isn't common.
			for (String comparator : nbtComparators.keySet())
			{
				if (name.contains(comparator))
				{
					valueChecker = nbtComparators.get(comparator);
					String[] components = name.split(comparator);
					name = components[0];
					if (components.length > 1)
					{
						value = components[1];
					}
					break;
				}
			}

			// Look for a tag matching the given name and value.
			Tag itemTag = item.save(provider);

			boolean result = findMatchingSubtag(itemTag, name, value, valueChecker);

			if (!result)
			{
				// If we didn't find any matching subtags, try adding the minecraft namespace to all keys and values that don't have one specified.
				if (!name.contains(":"))
				{
					name = "minecraft:" + name;
				}

				// For the value, there are a few different possible scenarios:
				// 1. The value is just an alphabetical string value, and the namespace can be appended directly.
				// 2. The value represents a compound tag or list tag, and any alphabetical string values in quotes can have the namespace appended.
				if (value != null)
				{
					if (!value.contains(":") && value.matches("^[a-z]+$"))
					{
						value = "minecraft:" + value;
					}
					else if (value.contains("\""))
					{
						// We need to check for the presence of quotes, and if they are present, we need to add the namespace to the value.
						String[] components = value.split("\"");
						for (int i = 0; i < components.length; i++)
						{
							if (i % 2 == 1 && !components[i].contains(":"))
							{
								components[i] = "minecraft:" + components[i];
							}
						}
						value = String.join("\"", components);
					}
				}

				// Try again with the new values.
				result = findMatchingSubtag(itemTag, name, value, valueChecker);
			}

			return result;
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

		// If this tag represents a string value, first check if that string can be serialized into a real tag.
		if (tag.getId() == Tag.TAG_STRING)
		{
			try
			{
				tag = TagParser.parseTag(tag.getAsString());
			}
			catch (Exception e)
			{
				// Nope!  Keep going.
			}
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
					else if (compoundTag.getTagType(innerKey) == Tag.TAG_STRING)
					{
						try
						{
							tag = TagParser.parseTag(tag.getAsString());
							if (findMatchingSubtag(compoundTag.get(innerKey), key, value, valueChecker))
							{
								return true;
							}
						}
						catch (Exception e)
						{
							// Nope!  Keep going.
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