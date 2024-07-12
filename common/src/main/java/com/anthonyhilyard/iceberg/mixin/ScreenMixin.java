package com.anthonyhilyard.iceberg.mixin;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.services.Services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

@Mixin(Screen.class)
public class ScreenMixin
{
	@Inject(method = "getTooltipFromItem", at = @At(value = "HEAD"))
	private static List<Component> getTooltipFromItem(Minecraft minecraft, ItemStack itemStack, CallbackInfoReturnable<List<Component>> info)
	{
		if (Services.PLATFORM.isModLoaded("andromeda"))
		{
			try
			{
				Field tooltipStackField = GuiGraphics.class.getDeclaredField("icebergTooltipStack");
				tooltipStackField.setAccessible(true);
				tooltipStackField.set(null, itemStack);
			}
			catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
			{
				Iceberg.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return info.getReturnValue();
	}
}
