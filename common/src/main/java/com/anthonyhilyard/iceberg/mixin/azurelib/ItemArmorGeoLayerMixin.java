package com.anthonyhilyard.iceberg.mixin.azurelib;

import java.lang.reflect.Field;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.anthonyhilyard.iceberg.Iceberg;

import mod.azure.azurelib.common.api.client.renderer.layer.ItemArmorGeoLayer;
import net.minecraft.client.renderer.MultiBufferSource;


@SuppressWarnings("removal")
@Mixin(value = ItemArmorGeoLayer.class, remap = false)
public class ItemArmorGeoLayerMixin
{
	@Unique
	Field bufferSourceFieldV2 = null;

	@Unique
	Field bufferSourceFieldV3 = null;

	@ModifyVariable(method = "renderforBone", at = @At("LOAD"), argsOnly = true, index = 5, require = 0)
	private MultiBufferSource icebergStoreBufferSource(MultiBufferSource bufferSource)
	{
		try
		{
			if (bufferSourceFieldV2 == null)
			{
				bufferSourceFieldV2 = Class.forName("mod.azure.azurelib.common.api.client.renderer.GeoArmorRenderer").getDeclaredField("bufferSource");
				bufferSourceFieldV2.setAccessible(true);
			}

			if (bufferSourceFieldV2 != null)
			{
				bufferSourceFieldV2.set(null, bufferSource);
			}
		}
		catch (Exception e)
		{
			Iceberg.LOGGER.debug(ExceptionUtils.getStackTrace(e));
		}

		try
		{
			if (bufferSourceFieldV3 == null)
			{
				bufferSourceFieldV3 = Class.forName("mod.azure.azurelib.rewrite.render.armor.AzArmorModel").getDeclaredField("bufferSource");
				bufferSourceFieldV3.setAccessible(true);
			}

			if (bufferSourceFieldV3 != null)
			{
				bufferSourceFieldV3.set(null, bufferSource);
			}
		}
		catch (Exception e)
		{
			Iceberg.LOGGER.debug(ExceptionUtils.getStackTrace(e));
		}
		return bufferSource;
	}
}
