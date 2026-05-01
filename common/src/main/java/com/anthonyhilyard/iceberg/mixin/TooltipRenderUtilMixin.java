package com.anthonyhilyard.iceberg.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.anthonyhilyard.iceberg.util.Tooltips;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin
{
	/**
	 * //TODO tooltips
	 * 1.21.11 renders tooltips as full sprites, disabled for now.
	 * Recommended fix is to write an entire new tooltip rendering engine.
	 */
	/*
	@Redirect(method = "renderTooltipBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 0))
	private static void icebergRenderBackground(GuiGraphics instance, Function<Identifier, RenderType> renderTypeLookup, Identifier sprite, int adjustedX, int adjustedY, int adjustedWidth, int adjustedHeight, GuiGraphics guiGraphics, int x, int y, int width, int height, int z)
	{
		if (Tooltips.gradientBackground)
		{
			instance.pose().pushPose();
			instance.pose().translate(0.0f, 0.0f, -z);
			Tooltips.renderGradientBackground(instance, x, y, width, height, z, Tooltips.currentColors.backgroundColorStart().getValue(), Tooltips.currentColors.backgroundColorEnd().getValue());
			instance.pose().popPose();
		}
		else
		{
			instance.blitSprite(renderTypeLookup, sprite, adjustedX, adjustedY, adjustedWidth, adjustedHeight);
		}
	}

	@Redirect(method = "renderTooltipBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 1))
	private static void icebergRenderBorder(GuiGraphics instance, Function<Identifier, RenderType> renderTypeLookup, Identifier sprite, int adjustedX, int adjustedY, int adjustedWidth, int adjustedHeight, GuiGraphics guiGraphics, int x, int y, int width, int height, int z)
	{
		if (Tooltips.gradientBorder)
		{
			instance.pose().pushPose();
			instance.pose().translate(0.0f, 0.0f, -z);
			Tooltips.renderGradientBorder(instance, x, y, width, height, z, Tooltips.currentColors.borderColorStart().getValue(), Tooltips.currentColors.borderColorEnd().getValue());
			instance.pose().popPose();
		}
		else
		{
			instance.blitSprite(renderTypeLookup, sprite, adjustedX, adjustedY, adjustedWidth, adjustedHeight);
		}
	}
	*/
}