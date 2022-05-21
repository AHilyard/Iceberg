package com.anthonyhilyard.iceberg.mixin;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;

import com.anthonyhilyard.iceberg.Loader;
import com.mojang.blaze3d.pipeline.RenderTarget;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin
{
	@SuppressWarnings("unchecked")
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear(Z)V", ordinal = 0), require = 0)
	public void fixUpdateURLs(RenderTarget renderTarget, boolean onOSX)
	{
		for (IModInfo mod : FMLLoader.getLoadingModList().getMods())
		{
			Optional<Object> authors = mod.getConfig().getConfigElement("authors");
			if (!authors.isPresent())
			{
				continue;
			}

			boolean madeByGrend = false;
			if (authors.get() instanceof String author)
			{
				madeByGrend = author.contentEquals("Grend");
			}
			else if (authors.get() instanceof ArrayList<?> authorList)
			{
				madeByGrend = authorList.contains("Grend");
			}

			if (madeByGrend)
			{
				// Found a mod I made, so patch the update URL.
				// Yeah, it's a dirty hack but it lets me fix all my mods at once before I have the time to update them all properly.
				ModInfo modInfo = (ModInfo)mod;
				
				try
				{
					Field updateJSONURLField = ModInfo.class.getDeclaredField("updateJSONURL");
					updateJSONURLField.setAccessible(true);
					
					Optional<URL> updateJSONURL = (Optional<URL>)updateJSONURLField.get(modInfo);
					if (updateJSONURL.isPresent())
					{
						String url = updateJSONURL.get().toString();
						url = url.replace("mc-curse-update-checker.herokuapp.com", "mc-update-check.anthonyhilyard.com");
						updateJSONURLField.set(modInfo, Optional.of(new URL(url)));
						
					}
				}
				catch (Exception e)
				{
					Loader.LOGGER.debug(ExceptionUtils.getStackTrace(e));
				}
			}

		}
		renderTarget.clear(onOSX);
	}
}
