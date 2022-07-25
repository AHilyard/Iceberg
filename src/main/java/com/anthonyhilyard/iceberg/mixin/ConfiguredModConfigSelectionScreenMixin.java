package com.anthonyhilyard.iceberg.mixin;

import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;

import com.anthonyhilyard.iceberg.Loader;
import com.anthonyhilyard.iceberg.config.IcebergConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mrcrayfish.configured.client.screen.ConfigScreen;
import com.mrcrayfish.configured.client.screen.ListMenuScreen;
import com.mrcrayfish.configured.client.screen.ModConfigSelectionScreen;
import com.mrcrayfish.configured.client.screen.ModConfigSelectionScreen.FileItem;
import com.mrcrayfish.configured.client.screen.WorldSelectionScreen;
import com.mrcrayfish.configured.client.screen.ConfigScreen.FolderEntry;
import com.mrcrayfish.configured.client.screen.ConfigScreen.IEntry;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.util.ConfigHelper;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.ScreenNarrationCollector;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.unsafe.UnsafeHacks;

@Mixin(FileItem.class)
public class ConfiguredModConfigSelectionScreenMixin
{
	@Shadow(aliases = "this$0", remap = false)
	@Final
	ModConfigSelectionScreen this$0;

	@Shadow(remap = false)
	@Final
	protected ModConfig config;

	@Shadow(remap = false)
	@Final
	protected Component title;

	@Shadow(remap = false)
	private boolean hasRequiredPermission() { return true; }

	@Inject(method = "createModifyButton", at = @At(value = "HEAD"), remap = false, cancellable = true, require = 0)
	private void createModifyButton(ModConfig config, CallbackInfoReturnable<Button> info)
	{
		if (config.getSpec() instanceof IcebergConfigSpec)
		{
			Minecraft minecraft = Minecraft.getInstance();
			boolean serverConfig = config.getType() == ModConfig.Type.SERVER && Minecraft.getInstance().level == null;
			String langKey = serverConfig ? "configured.gui.select_world" : "configured.gui.modify";

			ResourceLocation backgroundTemp = null;
			try
			{
				Field backgroundField = ListMenuScreen.class.getDeclaredField("background");
				backgroundField.setAccessible(true);
				backgroundTemp = (ResourceLocation) backgroundField.get(this$0);
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}

			final ResourceLocation background = backgroundTemp;
			
			info.setReturnValue(new IconButton(0, 0, serverConfig ? 44 : 33, 0, serverConfig ? 80 : 60, Component.translatable(langKey), onPress ->
			{
				if (ConfigScreen.isPlayingGame() && this.config.getType() == ModConfig.Type.SERVER && (!ConfigHelper.isConfiguredInstalledOnServer() || !this.hasRequiredPermission()))
				{
					return;
				}

				if (serverConfig)
				{
					minecraft.setScreen(new WorldSelectionScreen(this$0, background, config, this.title));
				}
				else
				{
					ModList.get().getModContainerById(config.getModId()).ifPresent(container ->
					{
						try
						{
							ConfigScreen configScreen = UnsafeHacks.newInstance(ConfigScreen.class);

							Field titleField = Screen.class.getDeclaredField("f_96539_");
							Field childrenField = Screen.class.getDeclaredField("f_96540_");
							Field narratablesField = Screen.class.getDeclaredField("f_169368_");
							Field renderablesField = Screen.class.getDeclaredField("f_169369_");
							Field narrationStateField = Screen.class.getDeclaredField("f_169375_");
							Field parentField = configScreen.getClass().getSuperclass().getDeclaredField("parent");
							Field backgroundField = configScreen.getClass().getSuperclass().getDeclaredField("background");
							Field itemHeightField = configScreen.getClass().getSuperclass().getDeclaredField("itemHeight");
							titleField.setAccessible(true);
							childrenField.setAccessible(true);
							narratablesField.setAccessible(true);
							renderablesField.setAccessible(true);
							narrationStateField.setAccessible(true);
							parentField.setAccessible(true);
							backgroundField.setAccessible(true);
							itemHeightField.setAccessible(true);

							titleField.set(configScreen, Component.literal(container.getModInfo().getDisplayName()));
							childrenField.set(configScreen, Lists.newArrayList());
							narratablesField.set(configScreen, Lists.newArrayList());
							renderablesField.set(configScreen, Lists.newArrayList());
							narrationStateField.set(configScreen, new ScreenNarrationCollector());
							parentField.set(configScreen, this$0);
							backgroundField.set(configScreen, background);
							itemHeightField.set(configScreen, 24);

							ForgeConfigSpec dummySpec = new ForgeConfigSpec.Builder().build();
							FieldUtils.writeDeclaredField(configScreen, "folderEntry", new IcebergFolderEntry(configScreen, List.of(), ((IcebergConfigSpec) config.getSpec()).getValues(), dummySpec, (IcebergConfigSpec) config.getSpec()), true);
							FieldUtils.writeDeclaredField(configScreen, "config", config, true);

							minecraft.setScreen(configScreen);
						}
						catch (Exception e)
						{
							Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
						}
					});
				}
			}, (button, poseStack, mouseX, mouseY) ->
			{
				if (button.isHoveredOrFocused())
				{
					if (ConfigScreen.isPlayingGame() && !ConfigHelper.isConfiguredInstalledOnServer())
					{
						this$0.renderTooltip(poseStack, minecraft.font.split(Component.translatable("configured.gui.not_installed").withStyle(ChatFormatting.RED), Math.max(this$0.width / 2 - 43, 170)), mouseX, mouseY);
					}
					else if (!this.hasRequiredPermission())
					{
						this$0.renderTooltip(poseStack, minecraft.font.split(Component.translatable("configured.gui.no_permission").withStyle(ChatFormatting.RED), Math.max(this$0.width / 2 - 43, 170)), mouseX, mouseY);
					}
				}
			}));
		}
	}

	public class IcebergFolderEntry extends FolderEntry
	{
		private IcebergConfigSpec icebergConfigSpec;
		private ForgeConfigSpec spec;
		private ConfigScreen configScreen;
		private UnmodifiableConfig config;
		private final List<String> path;
		public IcebergFolderEntry(ConfigScreen configScreen, List<String> path, UnmodifiableConfig config, ForgeConfigSpec spec, IcebergConfigSpec icebergSpec)
		{
			configScreen.super(path, config, spec);
			icebergConfigSpec = icebergSpec;
			this.spec = spec;
			this.configScreen = configScreen;
			this.config = config;
			this.path = path;
			init();
		}

		private void init()
		{
			try
			{
				Field entriesField = getClass().getSuperclass().getDeclaredField("entries");
				entriesField.setAccessible(true);

				ImmutableList.Builder<IEntry> builder = ImmutableList.builder();
				config.valueMap().forEach((key, value) ->
				{
					if (value instanceof ForgeConfigSpec.ValueSpec valueSpec && valueSpec.getDefault() instanceof UnmodifiableConfig)
					{
						value = valueSpec.getDefault();
					}

					if (value instanceof UnmodifiableConfig)
					{
						List<String> path = new ArrayList<>(this.path);
						path.add(key);
						builder.add(new IcebergFolderEntry(configScreen, path, (UnmodifiableConfig) value, spec, icebergConfigSpec));
					}
					else if (value instanceof ForgeConfigSpec.ConfigValue<?>)
					{
						ForgeConfigSpec.ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>) value;
						ForgeConfigSpec.ValueSpec valueSpec = icebergConfigSpec.getRaw(configValue.getPath());
						if (valueSpec != null)
						{
							builder.add(configScreen.new ValueEntry(configValue, valueSpec));
						}
					}
				});
				entriesField.set(this, builder.build());
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
				return;
			}
		}
	}
}
