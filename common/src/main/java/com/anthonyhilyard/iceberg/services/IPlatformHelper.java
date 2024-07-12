package com.anthonyhilyard.iceberg.services;

import java.util.List;

public interface IPlatformHelper
{
	/**
	 * Gets the name of the current platform
	 *
	 * @return The name of the current platform.
	 */
	String getPlatformName();

	/**
	 * Checks if a mod with the given id is loaded.
	 *
	 * @param modId The mod to check if it is loaded.
	 * @return True if the mod is loaded, false otherwise.
	 */
	boolean isModLoaded(String modId);

	/**
	 * Gets a list of ids for all loaded mods.
	 *
	 * @return A list of ids for all loaded mods.
	 */
	List<String> getAllModIds();

	/**
	 * Checks if a loaded mod has the given version or higher.
	 * 
	 * @param modId The mod to check the version of.
	 * @param versionString The string representing the version to check against.
	 * @return True if the given mod is loaded and has a version equal to or greater than the given version.
	 */
	boolean modVersionMeets(String modId, String versionString);
}