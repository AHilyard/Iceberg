package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.anthonyhilyard.iceberg.component.IExtendedText;
import com.anthonyhilyard.iceberg.util.Tooltips;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;

@Mixin(ClientTextTooltip.class)
public class ClientTextTooltipMixin implements IExtendedText
{
	@Unique
	Font currentFont;

	@Unique
	TextAlignment textAlignment = TextAlignment.LEFT;

	@Unique
	int leftPadding = 0;

	@Unique
	int rightPadding = 0;

	@Unique
	int topPadding = 0;

	@Unique
	int bottomPadding = 0;

	@Override
	public void setAlignment(TextAlignment alignment) { textAlignment = alignment; }

	@Override
	public void setPadding(int left, int right, int top, int bottom) { leftPadding = left; rightPadding = right; topPadding = top; bottomPadding = bottom; }

	@Override
	public TextAlignment getAlignment() { return textAlignment; }

	@Override
	public int getLeftPadding() { return leftPadding; }

	@Override
	public int getRightPadding() { return rightPadding; }

	@Override
	public int getTopPadding() { return topPadding; }

	@Override
	public int getBottomPadding() { return bottomPadding; }

	@ModifyVariable(method = "renderText", at = @At(value = "LOAD"), argsOnly = true, index = 1)
	private Font getFont(Font font)
	{
		currentFont = font;
		return font;
	}

	@ModifyVariable(method = "renderText", at = @At(value = "LOAD"), argsOnly = true, index = 2)
	private int modifyHorizontalOffset(int xOriginal)
	{
		if (currentFont == null)
		{
			return xOriginal;
		}

		int tooltipWidth = Tooltips.getCurrentRect().getWidth();
		return xOriginal + Tooltips.getTitleOffset(tooltipWidth, currentFont.width(((ClientTextTooltip)(Object)this).text), leftPadding, rightPadding, textAlignment);
	}

	@ModifyVariable(method = "renderText", at = @At(value = "LOAD"), argsOnly = true, index = 3)
	private int modifyVerticalOffset(int yOriginal)
	{
		if (currentFont == null)
		{
			return yOriginal;
		}

		return yOriginal + topPadding;
	}

	@Inject(method = "getWidth", at = @At("RETURN"), cancellable = true)
	private void adjustWidth(Font font, CallbackInfoReturnable<Integer> info)
	{
		info.setReturnValue(Tooltips.getTitleWidth((ClientTextTooltip)(Object)this, font));
	}

	@Inject(method = "getHeight", at = @At("RETURN"), cancellable = true)
	private void adjustHeight(CallbackInfoReturnable<Integer> info)
	{
		int defaultHeight = 10;
		int result = topPadding + defaultHeight + bottomPadding;
		info.setReturnValue(result);
	}

}
