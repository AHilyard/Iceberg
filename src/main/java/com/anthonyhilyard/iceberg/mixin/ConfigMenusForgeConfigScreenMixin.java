package com.anthonyhilyard.iceberg.mixin;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.anthonyhilyard.iceberg.Loader;
import com.anthonyhilyard.iceberg.util.ConfigMenusForgeHelper;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fuzs.configmenusforge.client.gui.data.IEntryData;
import fuzs.configmenusforge.client.gui.screens.ConfigScreen;
import fuzs.configmenusforge.client.util.ServerConfigUploader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;


@Mixin(ConfigScreen.class)
public abstract class ConfigMenusForgeConfigScreenMixin extends Screen
{
	protected ConfigMenusForgeConfigScreenMixin(Component p_96550_) { super(p_96550_); }
	
	@Unique
	private UnmodifiableConfig mainConfig = null;

	@Inject(method = "create", at = @At("HEAD"), remap = false, cancellable = true)
	private static void create(Screen lastScreen, Component title, ResourceLocation background, ModConfig config, Map<Object, IEntryData> valueToData, CallbackInfoReturnable<ConfigScreen> info)
	{
		try
		{
			Constructor<?> mainConstructor = Class.forName("fuzs.configmenusforge.client.gui.screens.ConfigScreen$Main").getDeclaredConstructor(Screen.class, Component.class, ResourceLocation.class, UnmodifiableConfig.class, Map.class, Runnable.class);
			mainConstructor.setAccessible(true);
			info.setReturnValue((ConfigScreen)mainConstructor.newInstance(lastScreen, title, background, ConfigMenusForgeHelper.getValues(config.getSpec()), valueToData, (Runnable)(() -> ServerConfigUploader.saveAndUpload(config))));
			info.cancel();
			return;
		}
		catch (Exception e)
		{
			Loader.LOGGER.warn(ExceptionUtils.getStackTrace(e.getCause()));
		}
	}

	@Redirect(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/resources/ResourceLocation;Lcom/electronwill/nightconfig/core/UnmodifiableConfig;Ljava/util/Map;[I)V",
			  at = @At(value = "INVOKE", target = "Ljava/util/Collection;stream()Ljava/util/stream/Stream;", ordinal = 0, remap = false), remap = false)
	Stream<Object> filteredEntries(Collection<Object> values)
	{
		return values.stream().map(value -> {
			if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue && configValue.get() instanceof UnmodifiableConfig config)
			{
				return config;
			}
			else
			{
				return value;
			}
		});
	}

	/// TODO: Add extended support for mutable subconfigs by adding an "Add new key" button and ability to delete keys.

	// @Shadow(remap = false)
	// @Final
	// @Mutable
	// private List<IEntryData> searchEntries;

	// @Shadow(remap = false)
	// @Final
	// @Mutable
	// private List<IEntryData> screenEntries;

	// @Shadow(remap = false)
	// @Final
	// @Mutable
	// Map<Object, IEntryData> valueToData;

	// @Shadow(remap = false)
	// EditBox searchTextField;

	// @Shadow(remap = false)
	// @Final
	// ResourceLocation background;
	
	// @Shadow(remap = false)
	// List<ConfigScreen.Entry> getConfigListEntries(List<IEntryData> entries, final String searchHighlight) { return null; }
	
	// @Inject(method = "getConfigListEntries(Ljava/lang/String;)Ljava/util/List;", at = @At("HEAD"), remap = false, cancellable = true)
	// private void getConfigListEntries(String query, CallbackInfoReturnable<List<ConfigScreen.Entry>> info)
	// {
	// 	query = query.toLowerCase(Locale.ROOT).trim();
	// 	if (query.isEmpty())
	// 	{
	// 		List<ConfigScreen.Entry> entries = Lists.newArrayList(getConfigListEntries(screenEntries, query));

	// 		// Add an "add new key" button if this is a dynamic subconfig.  We can't be sure that's what this is,
	// 		// since we don't have access to the spec here, so we're going to have to make an assumption...
	// 		try
	// 		{
	// 			if (mainConfig != null && mainConfig.getClass().isAssignableFrom(Class.forName("com.electronwill.nightconfig.core.SimpleCommentedConfig")))
	// 			{
	// 				Class<?> categoryEntryClass = Class.forName("fuzs.configmenusforge.client.gui.screens.ConfigScreen$CategoryEntry");

	// 				Constructor<?> categoryEntryConstructor = categoryEntryClass.getDeclaredConstructor(ConfigScreen.class, CategoryEntryData.class, String.class);
	// 				categoryEntryConstructor.setAccessible(true);
	// 				ConfigScreen.Entry addNewKeyEntry = (ConfigScreen.Entry) categoryEntryConstructor.newInstance(this, new CategoryEntryData(null, null, null) {
	// 					// TODO: Make translatable
	// 					private static Component title = new TextComponent("Add new key");
	// 					@Override
	// 					public String getPath() { return null; }
	// 					@Override
	// 					public String getComment() { return null; }
	// 					@Override
	// 					public Component getTitle() { return title; }
	// 					@Override
	// 					public boolean mayResetValue() { return false; }
	// 					@Override
	// 					public boolean mayDiscardChanges() { return false; }
	// 					@Override
	// 					public void resetCurrentValue() { }
	// 					@Override
	// 					public void discardCurrentValue() { }
	// 					@Override
	// 					public void saveConfigValue() { }
	// 					@Override
	// 					public boolean category() { return false; }
	// 				}, null);

	// 				Field buttonField = categoryEntryClass.getDeclaredField("button");
	// 				UnsafeHacks.setField(buttonField, addNewKeyEntry, new Button(10, 5, 260, 20, new TextComponent("Add new key"), button -> {
	// 					searchTextField.setValue("");
	// 					searchTextField.setFocus(false);
	// 					Screen editScreen = new EditStringScreen((ConfigScreen)(Object)this, title, background, "", x -> true, currentValue -> {
	// 						((Config)mainConfig).set(currentValue, "");
	// 						// Update screen and search entries lists.
	// 						List<IEntryData> newEntries = Lists.newArrayList();
	// 						ValueSpec newValueSpec = IcebergConfigSpec.createValueSpec(null, null, false, Object.class, () -> null, v -> v != null);
	// 						final EntryData.ConfigEntryData<?> data = new DynamicConfigEntryData<>(List.of(currentValue), "", newValueSpec, mainConfig);
	// 						valueToData = Maps.newLinkedHashMap(valueToData);
	// 						valueToData.put(currentValue, data);
	// 						gatherEntries(mainConfig, newEntries, valueToData);
	// 						searchEntries = newEntries;
	// 						screenEntries = mainConfig.valueMap().values().stream().map(valueToData::get).toList();
	// 						((ConfigScreen)(Object)this).updateList(false);
	// 					});
	// 					final Minecraft minecraft = Minecraft.getInstance();
	// 					minecraft.setScreen(editScreen);
	// 				}));
					
	// 				entries.add(addNewKeyEntry);
	// 			}
	// 		}
	// 		catch (Exception e)
	// 		{
	// 			Loader.LOGGER.info(ExceptionUtils.getStackTrace(e));
	// 		}
			
	// 		info.setReturnValue(entries);
	// 	}
	// 	else
	// 	{
	// 		info.setReturnValue(getConfigListEntries(searchEntries, query));
	// 	}
		
	// 	info.cancel();
	// }

	@Inject(method = "gatherEntriesRecursive(Lcom/electronwill/nightconfig/core/UnmodifiableConfig;Ljava/util/Map;)Ljava/util/List;",
			at = @At("HEAD"), remap = false, cancellable = true)
	private void gatherEntriesRecursiveSubconfigSupport(UnmodifiableConfig mainConfig, Map<Object, IEntryData> allEntries, CallbackInfoReturnable<List<IEntryData>> info)
	{
		// Store this config for later.
		this.mainConfig = mainConfig;

		List<IEntryData> entries = Lists.newArrayList();
		gatherEntries(mainConfig, entries, allEntries);
		info.setReturnValue(ImmutableList.copyOf(entries));
		info.cancel();
	}

	@Unique
	private static void gatherEntries(UnmodifiableConfig mainConfig, List<IEntryData> entries, Map<Object, IEntryData> entryMap)
	{
		for (Object value : mainConfig.valueMap().values())
		{
			if (entryMap.get(value) != null)
			{
				entries.add(entryMap.get(value));
			}
			if (value instanceof UnmodifiableConfig config)
			{
				gatherEntries(config, entries, entryMap);
			}
			else if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue && configValue.get() instanceof UnmodifiableConfig config)
			{
				if (entryMap.get(config) != null)
				{
					entries.add(entryMap.get(config));
				}
				gatherEntries(config, entries, entryMap);
			}
		}
	}
}
