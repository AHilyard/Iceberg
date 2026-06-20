package com.anthonyhilyard.iceberg.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.util.Tooltips;

import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin
{
	@Inject(method = "extractTooltipBackground", at = @At("HEAD"), cancellable = true)
	private static void replaceTooltipBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Identifier identifier, CallbackInfo ci)
	{
		if (Tooltips.gradientBackground || Tooltips.gradientBorder)
		{

			if (Tooltips.gradientBackground)
			{
				Tooltips.renderGradientBackground(
						graphics, x, y, width, height,
						Tooltips.currentColors.backgroundColorStart().getValue(),
						Tooltips.currentColors.backgroundColorEnd().getValue()
				);
			}

			if (Tooltips.gradientBorder)
			{
				Tooltips.renderGradientBorder(
						graphics, x, y, width, height,
						Tooltips.currentColors.borderColorStart().getValue(),
						Tooltips.currentColors.borderColorEnd().getValue()
				);
			}

			ci.cancel();
		}
	}
}