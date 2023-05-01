package com.anthonyhilyard.iceberg.renderer;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import com.anthonyhilyard.iceberg.util.UnsafeUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.RenderType;

import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;

public class VertexCollectorSodium extends VertexCollector
{
	protected VertexCollectorSodium()
	{
		super();
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType)
	{
		return new VertexConsumerSodium()
		{
			@Override
			public VertexConsumer vertex(double x, double y, double z)
			{
				currentVertex.set((float) x, (float) y, (float) z);
				currentAlpha = defaultAlpha;
				return this;
			}

			@Override
			public VertexConsumer color(int r, int g, int b, int a)
			{
				currentAlpha = a;
				return this;
			}

			@Override
			public VertexConsumer uv(float u, float v) { return this; }

			@Override
			public VertexConsumer overlayCoords(int x, int y) { return this; }

			@Override
			public VertexConsumer uv2(int u, int v) { return this; }

			@Override
			public VertexConsumer normal(float x, float y, float z) { return this; }

			@Override
			public void endVertex()
			{
				if (currentAlpha >= 25)
				{
					vertices.add(new Vector3f(currentVertex));
				}
			}

			@Override
			public void defaultColor(int r, int g, int b, int a)
			{
				defaultAlpha = a;
			}

			@Override
			public void unsetDefaultColor()
			{
				defaultAlpha = 255;
			}

			@Override
			public void push(MemoryStack memoryStack, long pointer, int count, VertexFormatDescription format)
			{
				// Loop over each vertex, and add it to the list if it's opaque.
				// To determine this, we need to check the vertex format to find the vertex position and alpha.
				for (int i = 0; i < count; i++)
				{
					// Get the vertex position.
					float x = UnsafeUtil.readFloat(pointer + i * format.stride() + format.getElementOffset(CommonVertexAttribute.POSITION));
					float y = UnsafeUtil.readFloat(pointer + i * format.stride() + format.getElementOffset(CommonVertexAttribute.POSITION) + 4);
					float z = UnsafeUtil.readFloat(pointer + i * format.stride() + format.getElementOffset(CommonVertexAttribute.POSITION) + 8);

					// Get the vertex alpha.
					int a = UnsafeUtil.readByte(pointer + i * format.stride() + format.getElementOffset(CommonVertexAttribute.COLOR) + 3) & 0xFF;

					// Add the vertex to the list if it's opaque.
					if (a >= 25)
					{
						vertices.add(new Vector3f(x, y, z));
					}
				}
			}
		};
	}
}