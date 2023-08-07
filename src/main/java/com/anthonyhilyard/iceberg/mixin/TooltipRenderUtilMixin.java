package com.anthonyhilyard.iceberg.mixin;

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
import net.minecraft.network.chat.TextColor;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin
{
	@Unique
	private static TextColor horizontalLineColor;
	
	@Shadow
	@Final
	private static int BACKGROUND_COLOR;

	@Shadow
	@Final
	private static int BORDER_COLOR_TOP;

	@Shadow
	@Final
	private static int BORDER_COLOR_BOTTOM;

	@Inject(method = "renderTooltipBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderHorizontalLine(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", shift = At.Shift.BEFORE, ordinal = 0))
	private static void icebergRenderTooltipBackgroundOne(GuiGraphics painter, int x, int y, int width, int height, int z, CallbackInfo info)
	{
		horizontalLineColor = Tooltips.currentColors.backgroundColorStart();
	}

	@Inject(method = "renderTooltipBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderHorizontalLine(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", shift = At.Shift.BEFORE, ordinal = 1))
	private static void icebergRenderTooltipBackgroundTwo(GuiGraphics painter, int x, int y, int width, int height, int z, CallbackInfo info)
	{
		horizontalLineColor = Tooltips.currentColors.backgroundColorEnd();
	}

	@Inject(method = "renderFrameGradient", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderHorizontalLine(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", shift = At.Shift.BEFORE, ordinal = 0))
	private static void icebergRenderFrameGradientOne(GuiGraphics painter, int x, int y, int width, int height, int z, int color1, int color2, CallbackInfo info)
	{
		horizontalLineColor = Tooltips.currentColors.borderColorStart();
	}

	@Inject(method = "renderFrameGradient", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderHorizontalLine(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V", shift = At.Shift.BEFORE, ordinal = 1))
	private static void icebergRenderFrameGradientTwo(GuiGraphics painter, int x, int y, int width, int height, int z, int color1, int color2, CallbackInfo info)
	{
		horizontalLineColor = Tooltips.currentColors.borderColorEnd();
	}

	@Inject(method = "renderHorizontalLine", at = @At(value = "HEAD"), cancellable = true)
	private static void icebergRenderHorizontalLine(GuiGraphics painter, int x, int y, int width, int z, int color, CallbackInfo info)
	{
		if (color != BACKGROUND_COLOR && color != BORDER_COLOR_TOP && color != BORDER_COLOR_BOTTOM)
		{
			// Do default behavior so other mods that change colors can still work.
		}
		else
		{
			// Replace the rendered colors with the ones previously stored.
			int renderColor = horizontalLineColor.getValue();
			painter.fillGradient(x, y, x + width, y + 1, z, renderColor, renderColor);
			info.cancel();
		}
	}

	@Inject(method = "renderRectangle", at = @At(value = "HEAD"), cancellable = true)
	private static void icebergRenderRectangle(GuiGraphics painter, int x, int y, int width, int height, int z, int color, CallbackInfo info)
	{
		if (color != BACKGROUND_COLOR)
		{
			// Do default behavior so other mods that change colors can still work.
		}
		else
		{
			// Replace the rendered colors with the ones previously stored.
			painter.fillGradient(x, y, x + width, y + height, z, Tooltips.currentColors.backgroundColorStart().getValue(), Tooltips.currentColors.backgroundColorEnd().getValue());
			info.cancel();
		}
	}

	@Inject(method = "renderVerticalLine", at = @At(value = "HEAD"), cancellable = true)
	private static void icebergRenderVerticalLine(GuiGraphics painter, int x, int y, int height, int z, int color, CallbackInfo info)
	{
		if (color != BACKGROUND_COLOR)
		{
			// Do default behavior so other mods that change colors can still work.
		}
		else
		{
			// Replace the rendered colors with the ones previously stored.
			painter.fillGradient(x, y, x + 1, y + height, z, Tooltips.currentColors.backgroundColorStart().getValue(), Tooltips.currentColors.backgroundColorEnd().getValue());
			info.cancel();
		}
	}

	@Inject(method = "renderVerticalLineGradient", at = @At(value = "HEAD"), cancellable = true)
	private static void icebergRenderVerticalLineGradient(GuiGraphics painter, int x, int y, int height, int z, int startColor, int endColor, CallbackInfo info)
	{
		if (startColor != BORDER_COLOR_TOP || endColor != BORDER_COLOR_BOTTOM)
		{
			// Do default behavior so other mods that change colors can still work.
		}
		else
		{
			// Replace the rendered colors with the ones previously stored.
			painter.fillGradient(x, y, x + 1, y + height, z, Tooltips.currentColors.borderColorStart().getValue(), Tooltips.currentColors.borderColorEnd().getValue());
			info.cancel();
		}
	}
}