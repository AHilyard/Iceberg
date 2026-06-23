package com.anthonyhilyard.iceberg.mixin;

import com.anthonyhilyard.iceberg.util.INestedTooltipAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

@Mixin(ClientBundleTooltip.class)
public class ClientBundleTooltipMixin
{
	@Shadow @Final private BundleContents contents;

	@Inject(method = "extractSelectedItemTooltip", at = @At("HEAD"))
	private void captureNestedBundleItem(Font font, GuiGraphicsExtractor graphics, int x, int y, int w, CallbackInfo ci)
	{
		ItemStackTemplate innerItem = this.contents.getSelectedItem();
		if (innerItem != null)
		{
			((INestedTooltipAccess) graphics).setIcebergNestedTooltipStack(innerItem.create());
		}
	}

	@Inject(method = "extractSelectedItemTooltip", at = @At("TAIL"))
	private void releaseNestedBundleItem(Font font, GuiGraphicsExtractor graphics, int i, int j, int k, CallbackInfo ci)
	{
		((INestedTooltipAccess) graphics).setIcebergNestedTooltipStack(ItemStack.EMPTY);
	}
}