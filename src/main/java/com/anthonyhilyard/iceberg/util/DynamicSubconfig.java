package com.anthonyhilyard.iceberg.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.electronwill.nightconfig.core.AbstractCommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;

/**
 * An exact copy of SimpleCommentedConfig, but this class is specifically meant for subconfigs.
 * That being said--the class of a config is checked during config validation, and subconfigs are allowed
 * extra leniency in config keys.
 */
final public class DynamicSubconfig extends AbstractCommentedConfig
{
	private final ConfigFormat<?> configFormat;

	/**
	 * Creates a Subconfig with the specified format.
	 *
	 * @param configFormat the config's format
	 */
	DynamicSubconfig(ConfigFormat<?> configFormat, boolean concurrent)
	{
		super(concurrent ? new ConcurrentHashMap<>() : new HashMap<>());
		this.configFormat = configFormat;
	}

	/**
	 * Creates a Subconfig with the specified data and format. The map is used as it is and
	 * isn't copied.
	 */
	DynamicSubconfig(Map<String, Object> valueMap, ConfigFormat<?> configFormat)
	{
		super(valueMap);
		this.configFormat = configFormat;
	}
	
	/**
	 * Creates a Subconfig with the specified backing map supplier and format.
	 * 
	 * @param mapCreator the supplier for backing maps
	 * @param configFormat the config's format
	 */
	DynamicSubconfig(Supplier<Map<String, Object>> mapCreator, ConfigFormat<?> configFormat)
	{
		super(mapCreator);
		this.configFormat = configFormat;
	}

	/**
	 * Creates a Subconfig by copying a config and with the specified format.
	 *
	 * @param toCopy       the config to copy
	 * @param configFormat the config's format
	 */
	DynamicSubconfig(UnmodifiableConfig toCopy, ConfigFormat<?> configFormat,
						  boolean concurrent)
	{
		super(toCopy, concurrent);
		this.configFormat = configFormat;
	}
	
	/**
	 * Creates a Subconfig by copying a config, with the specified backing map creator and format.
	 *
	 * @param toCopy       the config to copy
	 * @param mapCreator   the supplier for backing maps
	 * @param configFormat the config's format
	 */
	public DynamicSubconfig(UnmodifiableConfig toCopy, Supplier<Map<String, Object>> mapCreator,
			ConfigFormat<?> configFormat)
	{
		super(toCopy, mapCreator);
		this.configFormat = configFormat;
	}

	/**
	 * Creates a Subconfig by copying a config and with the specified format.
	 *
	 * @param toCopy       the config to copy
	 * @param configFormat the config's format
	 */
	DynamicSubconfig(UnmodifiableCommentedConfig toCopy, ConfigFormat<?> configFormat,
						  boolean concurrent)
	{
		super(toCopy, concurrent);
		this.configFormat = configFormat;
	}
	
	/**
	 * Creates a Subconfig by copying a config, with the specified backing map creator and format.
	 *
	 * @param toCopy       the config to copy
	 * @param mapCreator   the supplier for backing maps
	 * @param configFormat the config's format
	 */
	public DynamicSubconfig(UnmodifiableCommentedConfig toCopy, Supplier<Map<String, Object>> mapCreator,
			ConfigFormat<?> configFormat)
	{
		super(toCopy, mapCreator);
		this.configFormat = configFormat;
	}

	@Override
	public ConfigFormat<?> configFormat()
	{
		return configFormat;
	}

	@Override
	public DynamicSubconfig createSubConfig()
	{
		return new DynamicSubconfig(mapCreator, configFormat);
	}

	@Override
	public AbstractCommentedConfig clone()
	{
		return new DynamicSubconfig(this, mapCreator, configFormat);
	}

	/**
	 * Creates a new Subconfig with the content of the given config. The returned config will have
	 * the same format as the copied config.
	 *
	 * @param config the config to copy
	 * @return a copy of the config
	 */
	static DynamicSubconfig copy(UnmodifiableConfig config)
	{
		return new DynamicSubconfig(config, config.configFormat(), false);
	}
}