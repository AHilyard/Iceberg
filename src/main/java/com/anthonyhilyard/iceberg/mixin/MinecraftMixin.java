package com.anthonyhilyard.iceberg.mixin;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;

import com.anthonyhilyard.iceberg.Loader;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin
{
	@SuppressWarnings("unchecked")
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;setClearColor(FFFF)V", ordinal = 0), require = 0)
	public void fixUpdateURLs(Framebuffer framebuffer, float r, float g, float b, float a)
	{
		for (IModInfo mod : FMLLoader.getLoadingModList().getMods())
		{
			ModInfo modInfo = (ModInfo)mod;
			Optional<Object> authors = modInfo.getConfigElement("authors");
			if (!authors.isPresent())
			{
				continue;
			}

			boolean madeByGrend = false;
			if (authors.get() instanceof String)
			{
				String author = (String)authors.get();
				madeByGrend = author.contentEquals("Grend");
			}
			else if (authors.get() instanceof ArrayList<?>)
			{
				ArrayList<String> authorList = (ArrayList<String>)authors.get();
				madeByGrend = authorList.contains("Grend");
			}

			if (madeByGrend)
			{
				// Found a mod I made, so patch the update URL.
				// Yeah, it's a dirty hack but it lets me fix all my mods at once before I have the time to update them all properly.
				try
				{
					Field updateJSONURLField = ModInfo.class.getDeclaredField("updateJSONURL");
					updateJSONURLField.setAccessible(true);
					
					URL updateJSONURL = (URL)updateJSONURLField.get(modInfo);
					if (updateJSONURL != null)
					{
						String url = updateJSONURL.toString();
						url = url.replace("mc-curse-update-checker.herokuapp.com", "mc-update-check.anthonyhilyard.com");
						updateJSONURLField.set(modInfo, new URL(url));
					}
				}
				catch (Exception e)
				{
					Loader.LOGGER.debug(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		framebuffer.setClearColor(r, g, b, a);
	}
}