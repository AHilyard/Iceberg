package com.anthonyhilyard.iceberg.mixin;

import com.anthonyhilyard.iceberg.util.INestedTooltipAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

@Mixin(ClientBundleTooltip.class)
public class ClientBundleTooltipMixin
{
	@Shadow @Final private BundleContents contents;

	@Inject(method = "drawSelectedItemTooltip", at = @At("HEAD"))
	private void captureNestedBundleItem(Font font, GuiGraphics guiGraphics, int i, int j, int k, CallbackInfo ci)
	{
		if (this.contents.hasSelectedItem())
		{
			ItemStack innerItem = this.contents.getItemUnsafe(this.contents.getSelectedItem());

			((INestedTooltipAccess) guiGraphics).setIcebergNestedTooltipStack(innerItem);
		}
	}

	@Inject(method = "drawSelectedItemTooltip", at = @At("TAIL"))
	private void releaseNestedBundleItem(Font font, GuiGraphics guiGraphics, int i, int j, int k, CallbackInfo ci)
	{
		((INestedTooltipAccess) guiGraphics).setIcebergNestedTooltipStack(ItemStack.EMPTY);
	}
}