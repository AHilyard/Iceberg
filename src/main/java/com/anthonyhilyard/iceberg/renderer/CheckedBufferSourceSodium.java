package com.anthonyhilyard.iceberg.renderer;

import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;



public final class CheckedBufferSourceSodium extends CheckedBufferSource
{
	protected CheckedBufferSourceSodium(MultiBufferSource bufferSource)
	{
		super(bufferSource);
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType)
	{
		final VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
		VertexConsumer vertexConsumerWrap = new VertexConsumerSodium()
		{
			@Override
			public VertexConsumer addVertex(float x, float y, float z)
			{
				hasRendered = true;
				return vertexConsumer.addVertex(x, y, z);
			}

			@Override
			public VertexConsumer setColor(int r, int g, int b, int a) { return vertexConsumer.setColor(r, g, b, a); }

			@Override
			public VertexConsumer setUv(float u, float v) { return vertexConsumer.setUv(u, v); }

			@Override
			public VertexConsumer setUv1(int u, int v) { return vertexConsumer.setUv1(u, v); }

			@Override
			public VertexConsumer setUv2(int u, int v) { return vertexConsumer.setUv2(u, v); }

			@Override
			public VertexConsumer setNormal(float x, float y, float z) { return vertexConsumer.setNormal(x, y, z); }

			@Override
			public void push(MemoryStack memoryStack, long pointer, int count, VertexFormatDescription format)
			{
				hasRendered = true;
				((VertexBufferWriter)vertexConsumer).push(memoryStack, pointer, count, format);
			}
		};

		return vertexConsumerWrap;
	}
}