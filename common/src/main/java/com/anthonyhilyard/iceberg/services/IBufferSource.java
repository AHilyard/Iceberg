package com.anthonyhilyard.iceberg.services;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

public interface IBufferSource
{
	VertexConsumer getBuffer(RenderType renderType);
}
