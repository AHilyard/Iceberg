package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.chat.TextColor;

@Mixin(TextColor.class)
public class TextColorMixin
{
	/**
	 * Fix an issue in TextColor parsing that makes it so only alpha values up to 0x7F are supported.
	 */
	@Inject(method = "parseColor", at = @At("HEAD"), cancellable = true)
	private static boolean parseColor(String colorString, CallbackInfoReturnable<TextColor> info)
	{
		if (!colorString.startsWith("#"))
		{
			return false;
		}

		try
		{
			int i = Integer.parseUnsignedInt(colorString.substring(1), 16);
			info.setReturnValue(TextColor.fromRgb(i));
			return true;
		}
		catch (NumberFormatException numberformatexception)
		{
			info.setReturnValue(null);
			return true;
		}
	}
}