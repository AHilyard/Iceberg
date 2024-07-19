package com.anthonyhilyard.iceberg.forge.mixin;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.util.Tooltips;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin
{
	@Shadow
	@Final
	private static int BACKGROUND_COLOR;

	@Unique
	private static Field horizontalLineColorField = null;

	@Inject(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderHorizontalLine(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", shift = At.Shift.BEFORE, ordinal = 0, remap = false), remap = false)
	private static void icebergRenderTooltipBackgroundOne(GuiGraphics graphics, int x, int y, int width, int height, int z, int backgroundTop, int backgroundBottom, int borderTop, int borderBottom, CallbackInfo info)
	{
		if (horizontalLineColorField == null)
		{
			try
			{
				horizontalLineColorField = TooltipRenderUtil.class.getDeclaredField("horizontalLineColor");
				horizontalLineColorField.setAccessible(true);
			}
			catch (Exception e) {}
		}
		
		try
		{
			horizontalLineColorField.set(null, Tooltips.currentColors.backgroundColorStart());
		}
		catch (Exception e) {}
		
	}

	@Inject(method = "renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderHorizontalLine(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", shift = At.Shift.BEFORE, ordinal = 1, remap = false), remap = false)
	private static void icebergRenderTooltipBackgroundTwo(GuiGraphics graphics, int x, int y, int width, int height, int z, int backgroundTop, int backgroundBottom, int borderTop, int borderBottom, CallbackInfo info)
	{
		try
		{
			horizontalLineColorField.set(null, Tooltips.currentColors.backgroundColorEnd());
		}
		catch (Exception e) {}
	}

	@Inject(method = "renderRectangle(Lnet/minecraft/client/gui/GuiGraphics;IIIIIII)V", at = @At(value = "HEAD"), cancellable = true, remap = false)
	private static void icebergRenderRectangle(GuiGraphics graphics, int x, int y, int width, int height, int z, int colorFrom, int colorTo, CallbackInfo info)
	{
		if (colorFrom != BACKGROUND_COLOR && colorTo != BACKGROUND_COLOR)
		{
			// Do default behavior so other mods that change colors can still work.
		}
		else
		{
			// Replace the rendered colors with the ones previously stored.
			graphics.fillGradient(x, y, x + width, y + height, z, Tooltips.currentColors.backgroundColorStart().getValue(), Tooltips.currentColors.backgroundColorEnd().getValue());
			info.cancel();
		}
	}
}