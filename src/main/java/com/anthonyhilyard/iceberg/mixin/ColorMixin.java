package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.text.Color;

@Mixin(Color.class)
public class ColorMixin
{
	/**
	 * Fix an issue in Color parsing that makes it so only alpha values up to 0x7F are supported.
	 */
	@Inject(method = "parseColor", at = @At("HEAD"), cancellable = true)
	private static boolean parseColor(String colorString, CallbackInfoReturnable<Color> info)
	{
		if (!colorString.startsWith("#"))
		{
			return false;
		}

		try
		{
			int i = Integer.parseUnsignedInt(colorString.substring(1), 16);
			info.setReturnValue(Color.fromRgb(i));
			return true;
		}
		catch (NumberFormatException numberformatexception)
		{
			info.setReturnValue(null);
			return true;
		}
	}
}