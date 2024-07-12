package com.anthonyhilyard.iceberg.fabric.config;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.common.ConfigEvents;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.google.common.collect.Maps;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FilenameUtils;

public class ConfigTracker
{
	private static class ConfigInfo
	{
		private final String filename;
		private final FabricIcebergConfigSpec configSpec;
		private final String modId;
		private CommentedConfig configData;

		ConfigInfo(String filename, FabricIcebergConfigSpec configSpec, String modId)
		{
			this.filename = filename;
			this.configSpec = configSpec;
			this.modId = modId;
		}

		public String getFilename() { return filename; }
		public FabricIcebergConfigSpec getSpec() { return configSpec; }
		public String getModId() { return modId; }
		public CommentedConfig getConfigData() { return configData; }
	
		void setConfigData(final CommentedConfig configData)
		{
			this.configData = configData;
			configSpec.setConfig(configData);
		}

		public void save()
		{
			if (configData instanceof FileConfig fileConfig)
			{
				fileConfig.save();
			}
		}
	}

	public static final ConfigTracker INSTANCE = new ConfigTracker();
	private final ConcurrentMap<String, ConfigInfo> fileMap = Maps.newConcurrentMap();
	private final ConcurrentMap<String, ConfigInfo> configsByMod = Maps.newConcurrentMap();

	private static final Path defaultConfigPath = FabricLoader.getInstance().getConfigDir();

	private ConfigTracker()
	{
	}

	public void registerConfig(FabricIcebergConfigSpec configSpec, String modId)
	{
		trackConfig(new ConfigInfo(String.format(Locale.ROOT, "%s.toml", modId), configSpec, modId));
	}

	private void trackConfig(final ConfigInfo config)
	{
		if (this.fileMap.containsKey(config.getFilename()))
		{
			Iceberg.LOGGER.error("Detected config file conflict {} between {} and {}", config.getFilename(), this.fileMap.get(config.getFilename()).getModId(), config.getModId());
			throw new RuntimeException("Config conflict detected!");
		}
		this.fileMap.put(config.getFilename(), config);
		this.configsByMod.put(config.getModId(), config);
		Iceberg.LOGGER.debug("Config file {} for {} tracking", config.getFilename(), config.getModId());
	}

	public void loadConfigs(Path configBasePath)
	{
		Iceberg.LOGGER.debug("Loading configs");
		fileMap.values().forEach(config -> openConfig(config, configBasePath));
	}

	public void unloadConfigs()
	{
		Iceberg.LOGGER.debug("Unloading configs");
		fileMap.values().forEach(this::closeConfig);
	}

	public void openConfig(final ConfigInfo config, final Path configBasePath)
	{
		Iceberg.LOGGER.trace("Loading config file at {} for {}", config.getFilename(), config.getModId());
		final CommentedFileConfig configData = readTomlFile(configBasePath, config);
		config.setConfigData(configData);

		ConfigEvents.LOAD.invoker().onLoad(config.getModId());
		config.save();
	}

	private CommentedFileConfig readTomlFile(Path basePath, final ConfigInfo config)
	{
		Path configPath = basePath.resolve(config.getFilename());

		final CommentedFileConfig configData = CommentedFileConfig.builder(configPath)
				.sync()
				.preserveInsertionOrder()
				.autosave()
				.onFileNotFound((newfile, configFormat) -> setupConfigFile(config, newfile, configFormat))
				.writingMode(WritingMode.REPLACE)
				.build();

		Iceberg.LOGGER.debug("Built TOML config for {}", configPath);
		try
		{
			configData.load();
		}
		catch (ParsingException ex)
		{
			Iceberg.LOGGER.warn("Attempting to recreate {}", configPath);
			try
			{
				backUpConfig(configData.getNioPath(), 3);
				Files.delete(configData.getNioPath());

				configData.load();
			}
			catch (Throwable t)
			{
				ex.addSuppressed(t);
				throw new RuntimeException("Failed loading config file " + config.getFilename() + " for modid " + config.getModId(), ex);
			}
		}

		Iceberg.LOGGER.debug("Loaded TOML config file {}", configPath);

		try
		{
			FileWatcher.defaultInstance().addWatch(configPath, new ConfigWatcher(config, configData, Thread.currentThread().getContextClassLoader()));
			Iceberg.LOGGER.debug("Watching TOML config file {} for changes", configPath);
		}
		catch (IOException e)
		{
			throw new RuntimeException("Couldn't watch config file", e);
		}
		return configData;
	}

	public void unload(ConfigInfo config)
	{
		Path configPath = getConfigFilePath(config.getModId());
		try
		{
			FileWatcher.defaultInstance().removeWatch(configPath);
		}
		catch (RuntimeException e)
		{
			Iceberg.LOGGER.error("Failed to remove config {} from tracker!", configPath, e);
		}
	}

	private boolean setupConfigFile(final ConfigInfo config, final Path file, final ConfigFormat<?> conf) throws IOException
	{
		Files.createDirectories(file.getParent());
		Path p = defaultConfigPath.resolve(config.getFilename());
		if (Files.exists(p))
		{
			Iceberg.LOGGER.debug("Loading default config file from path {}", p);
			Files.copy(p, file);
		}
		else
		{
			Files.createFile(file);
			conf.initEmptyFile(file);
		}
		return true;
	}

	public static void backUpConfig(final Path commentedFileConfig, final int maxBackups)
	{
		Path bakFileLocation = commentedFileConfig.getParent();
		String bakFileName = FilenameUtils.removeExtension(commentedFileConfig.getFileName().toString());
		String bakFileExtension = FilenameUtils.getExtension(commentedFileConfig.getFileName().toString()) + ".bak";
		Path bakFile = bakFileLocation.resolve(bakFileName + "-1" + "." + bakFileExtension);
		try {
			for (int i = maxBackups; i > 0; i--) {
				Path oldBak = bakFileLocation.resolve(bakFileName + "-" + i + "." + bakFileExtension);
				if (Files.exists(oldBak))
				{
					if (i >= maxBackups)
					{
						Files.delete(oldBak);
					}
					else
					{
						Files.move(oldBak, bakFileLocation.resolve(bakFileName + "-" + (i + 1) + "." + bakFileExtension));
					}
				}
			}
			Files.copy(commentedFileConfig, bakFile);
		}
		catch (IOException exception)
		{
			Iceberg.LOGGER.warn("Failed to back up config file {}", commentedFileConfig, exception);
		}
	}

	private void closeConfig(final ConfigInfo config) {
		if (config.getConfigData() != null)
		{
			Iceberg.LOGGER.trace("Closing config file at {} for {}", config.getFilename(), config.getModId());

			// Stop the filewatcher before we save the file and close it so reload doesn't fire.
			unload(config);

			config.save();
			config.setConfigData(null);
		}
	}

	public String getConfigFileName(String modId)
	{
		Path path = getConfigFilePath(modId);
		return path == null ? null : path.toString();
	}

	public Path getConfigFilePath(String modId)
	{
		return configsByMod.containsKey(modId) ? defaultConfigPath.resolve(configsByMod.get(modId).getFilename()) : null;
	}

	class ConfigWatcher implements Runnable
	{
		private final ConfigInfo configInfo;
		private final CommentedFileConfig commentedFileConfig;
		private final ClassLoader realClassLoader;

		ConfigWatcher(final ConfigInfo configInfo, final CommentedFileConfig commentedFileConfig, final ClassLoader classLoader)
		{
			this.configInfo = configInfo;
			this.commentedFileConfig = commentedFileConfig;
			this.realClassLoader = classLoader;
		}

		@Override
		public void run()
		{
			// Force the regular classloader onto this thread.
			Thread.currentThread().setContextClassLoader(realClassLoader);

			if (!configInfo.getSpec().applyCorrectionAction(commentedFileConfig, (spec, config) ->
				{
					Iceberg.LOGGER.warn("Configuration file {} is not correct. Correcting", config.getFile().getAbsolutePath());
					backUpConfig(config.getNioPath(), 3);
					spec.correct(config, (action, path, incorrectValue, correctedValue) -> {}, null);
				}, configInfo.getModId()))
			{
				throw new RuntimeException("Failed loading config file " + configInfo.getFilename() + " for modid " + configInfo.getModId());
			}
		}
	}

}
