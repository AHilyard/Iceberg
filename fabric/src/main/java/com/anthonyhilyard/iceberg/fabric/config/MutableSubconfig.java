package com.anthonyhilyard.iceberg.fabric.config;

import java.util.function.Predicate;

import com.anthonyhilyard.iceberg.fabric.config.FabricIcebergConfigSpec.BuilderContext;
import com.electronwill.nightconfig.core.AbstractCommentedConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.UnmodifiableConfig;

/**
 * This class is specifically meant for dynamic subconfigs--subconfigs where both keys and values are mutable.
 */
final class MutableSubconfig extends AbstractCommentedConfig
{
	private final ConfigFormat<?> configFormat;
	private final Predicate<Object> keyValidator;
	private final Predicate<Object> valueValidator;
	private static ValueSpec<?> defaultValueSpec = null;

	/**
	 * Creates a Subconfig by copying a config and with the specified format.
	 *
	 * @param toCopy       the config to copy
	 * @param configFormat the config's format
	 */
	MutableSubconfig(UnmodifiableConfig toCopy, ConfigFormat<?> configFormat, boolean concurrent, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
	{
		super(toCopy, concurrent);
		this.configFormat = configFormat;
		this.keyValidator = keyValidator;
		this.valueValidator = valueValidator;
	}

	// Returns a value spec to use for each entry in this subconfig.
	public ValueSpec<?> defaultValueSpec()
	{
		if (defaultValueSpec == null)
		{
			BuilderContext tmp = new BuilderContext();
			tmp.setClazz(Object.class);

			defaultValueSpec = new ValueSpec<Object>((() -> null), valueValidator, tmp, null);
		}
		return defaultValueSpec;
	}

	@Override
	public ConfigFormat<?> configFormat()
	{
		return configFormat;
	}

	public Predicate<Object> keyValidator()
	{
		return keyValidator;
	}

	public Predicate<Object> valueValidator()
	{
		return valueValidator;
	}

	/**
	 * Creates a new Subconfig with the content of the given config. The returned config will have
	 * the same format as the copied config.
	 *
	 * @param config the config to copy
	 * @return a copy of the config
	 */
	public static MutableSubconfig copy(UnmodifiableConfig config, Predicate<Object> keyValidator, Predicate<Object> valueValidator)
	{
		return new MutableSubconfig(config, config.configFormat(), false, keyValidator, valueValidator);
	}

	@Override
	public CommentedConfig createSubConfig() { throw new UnsupportedOperationException("Can't make a subconfig of a mutable subconfig!"); }

	@Override
	public AbstractCommentedConfig clone() { throw new UnsupportedOperationException("Can't clone a mutable subconfig!"); }
}