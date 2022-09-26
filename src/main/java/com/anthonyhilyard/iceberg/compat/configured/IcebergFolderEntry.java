package com.anthonyhilyard.iceberg.compat.configured;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.anthonyhilyard.iceberg.config.IcebergConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.ValueEntry;
import com.mrcrayfish.configured.impl.forge.ForgeEnumValue;
import com.mrcrayfish.configured.impl.forge.ForgeListValue;
import com.mrcrayfish.configured.impl.forge.ForgeValue;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

public class IcebergFolderEntry implements IConfigEntry
{
	protected final List<String> path;
	protected final UnmodifiableConfig config;
	protected final IcebergConfigSpec spec;
	private final String customComment;
	protected List<IConfigEntry> entries;

	public IcebergFolderEntry(UnmodifiableConfig config, IcebergConfigSpec spec)
	{
		this(new ArrayList<>(), config, spec);
	}

	public IcebergFolderEntry(List<String> path, UnmodifiableConfig config, IcebergConfigSpec spec)
	{
		this(path, config, spec, null);
	}

	public IcebergFolderEntry(List<String> path, UnmodifiableConfig config, IcebergConfigSpec spec, String customComment)
	{
		this.path = path;
		this.config = config;
		this.spec = spec;
		this.customComment = customComment;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<IConfigEntry> getChildren()
	{
		if (this.entries == null)
		{
			ImmutableList.Builder<IConfigEntry> builder = ImmutableList.builder();

			this.config.valueMap().forEach((key, value) ->
			{
				if (value instanceof ForgeConfigSpec.ValueSpec valueSpec && valueSpec.getDefault() instanceof UnmodifiableConfig)
				{
					value = valueSpec.getDefault();
				}

				if (value instanceof UnmodifiableConfig)
				{
					List<String> path = new ArrayList<>(this.path);
					path.add(key);
					builder.add(new IcebergFolderEntry(path, (UnmodifiableConfig) value, this.spec));
				}
				else if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue)
				{
					ForgeConfigSpec.ValueSpec valueSpec = this.spec.getRaw(configValue.getPath());
					if (valueSpec != null)
					{
						if (configValue.get() instanceof List<?>)
						{
							builder.add(new ValueEntry(new ForgeListValue((ForgeConfigSpec.ConfigValue<List<?>>) configValue, valueSpec)));
						}
						else if (configValue.get() instanceof Enum<?>)
						{
							builder.add(new ValueEntry(new ForgeEnumValue<>((ForgeConfigSpec.EnumValue<?>) configValue, valueSpec)));
						}
						// Configured doesn't currently support map configuration values, so leave a message for users.
						else if (configValue.get() instanceof UnmodifiableConfig)
						{
							List<String> path = new ArrayList<>(this.path);
							path.add(key);
							builder.add(new IcebergFolderEntry(path, (UnmodifiableConfig) configValue.get(), this.spec, "This is a map configuration value, which is not currently supported by Configured. To edit this value, please modify the configuration file directly."));
						}
						else
						{
							builder.add(new ValueEntry(new ForgeValue<>(configValue, valueSpec)));
						}
					}
				}
			});
			this.entries = builder.build();
		}
		return this.entries;
	}

	@Override
	public boolean isRoot()
	{
		return this.path.isEmpty();
	}

	@Override
	public boolean isLeaf()
	{
		return false;
	}

	@Override
	public IConfigValue<?> getValue()
	{
		return null;
	}

	@Override
	public String getEntryName()
	{
		return ForgeValue.lastValue(this.path, "Root");
	}

	@Nullable
	@Override
	public Component getTooltip()
	{
		if (customComment != null)
		{
			return Component.translatable(customComment);
		}
		else
		{
			String translationKey = this.getTranslationKey();
			if (translationKey != null)
			{
				String tooltipKey = translationKey + ".tooltip";
				if (I18n.exists(tooltipKey))
				{
					return Component.translatable(tooltipKey);
				}
			}
			String comment = this.spec.getLevelComment(this.path);
			if (comment != null)
			{
				return Component.literal(comment);
			}
			return null;
		}
	}

	@Nullable
	@Override
	public String getTranslationKey()
	{
		return this.spec.getLevelTranslationKey(this.path);
	}
	
}
