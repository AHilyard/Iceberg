package com.anthonyhilyard.iceberg.mixin;

import java.io.ByteArrayOutputStream;

import com.anthonyhilyard.iceberg.util.ConfigMenusForgeHelper;
import com.electronwill.nightconfig.toml.TomlFormat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fuzs.configmenusforge.ConfigMenusForge;
import fuzs.configmenusforge.client.util.ModConfigSync;
import fuzs.configmenusforge.client.util.ServerConfigUploader;
import fuzs.configmenusforge.network.client.message.C2SSendConfigMessage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.config.ModConfig;

@Mixin(ServerConfigUploader.class)
public class ConfigMenusForgeServerConfigUploaderMixin
{
	@Inject(method = "saveAndUpload", at = @At("HEAD"), remap = false, cancellable = true)
	private static void saveAndUpload(ModConfig config, CallbackInfo info)
	{
		ConfigMenusForgeHelper.save(config.getSpec());
		ModConfigSync.fireReloadingEvent(config);
		if (config.getType() == ModConfig.Type.SERVER)
		{
			final Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.getConnection() != null && !minecraft.isLocalServer())
			{
				final ByteArrayOutputStream stream = new ByteArrayOutputStream();
				TomlFormat.instance().createWriter().write(config.getConfigData(), stream);
				ConfigMenusForge.NETWORK.sendToServer(new C2SSendConfigMessage(config.getFileName(), stream.toByteArray()));
			}
		}
		info.cancel();
	}
}
