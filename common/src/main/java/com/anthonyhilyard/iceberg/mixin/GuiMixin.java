package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.util.Tooltips;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

@Mixin(Gui.class)
public class GuiMixin
{
	@Inject(method = "render", at = @At("HEAD"))
	private void resetTooltipsVisible(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo info)
	{
		Tooltips.setAnyTooltipsVisible(false);
	}
}
