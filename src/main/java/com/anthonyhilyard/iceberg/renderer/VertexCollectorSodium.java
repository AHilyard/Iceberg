package com.anthonyhilyard.iceberg.renderer;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import com.anthonyhilyard.iceberg.util.UnsafeUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.minecraft.client.renderer.RenderType;


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
			public VertexConsumer addVertex(float x, float y, float z)
			{
				if (currentAlpha >= 25)
				{
					vertices.add(new Vector3f(currentVertex.set(x, y, z)));
				}
				currentAlpha = 255;
				return this;
			}

			@Override
			public VertexConsumer setColor(int r, int g, int b, int a)
			{
				currentAlpha = a;
				return this;
			}

			@Override
			public VertexConsumer setUv(float u, float v) { return this; }

			@Override
			public VertexConsumer setUv1(int u, int v) { return this; }

			@Override
			public VertexConsumer setUv2(int u, int v) { return this; }

			@Override
			public VertexConsumer setNormal(float x, float y, float z) { return this; }

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