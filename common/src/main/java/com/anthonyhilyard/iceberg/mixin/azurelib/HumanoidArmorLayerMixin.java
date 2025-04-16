package com.anthonyhilyard.iceberg.mixin.azurelib;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin<T extends LivingEntity, A extends HumanoidModel<T>>
{
	@Unique
	Field bufferSourceFieldV2 = null;

	@Unique
	Field bufferSourceFieldV3 = null;

	@Inject(method = "render", require = 0, at = @At("HEAD"))
	public void icebergStoreBufferSource(PoseStack poseStack, MultiBufferSource bufferSource, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo callback)
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
		catch (Exception e) {}

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
		catch (Exception e) {}
	}
}
