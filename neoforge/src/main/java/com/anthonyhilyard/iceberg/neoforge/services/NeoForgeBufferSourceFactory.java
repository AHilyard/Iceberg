package com.anthonyhilyard.iceberg.neoforge.services;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import com.anthonyhilyard.iceberg.renderer.CheckedBufferSource;
import com.anthonyhilyard.iceberg.renderer.VertexCollector;
import com.anthonyhilyard.iceberg.services.IBufferSourceFactory;
import com.anthonyhilyard.iceberg.util.UnsafeUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class NeoForgeBufferSourceFactory implements IBufferSourceFactory
{

	@Override
	public CheckedBufferSource createCheckedBufferSource(Object bufferSource)
	{
		return new CheckedBufferSource((MultiBufferSource)bufferSource) {

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
					public void push(MemoryStack memoryStack, long pointer, int count, VertexFormat format)
					{
						hasRendered = true;
						((VertexBufferWriter)vertexConsumer).push(memoryStack, pointer, count, format);
					}
				};

				return vertexConsumerWrap;
			}
		};
	}

	@Override
	public VertexCollector createVertexCollector()
	{
		return new VertexCollector() {

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
					public void push(MemoryStack memoryStack, long pointer, int count, VertexFormat format)
					{
						// Loop over each vertex, and add it to the list if it's opaque.
						// To determine this, we need to check the vertex format to find the vertex position and alpha.
						for (int i = 0; i < count; i++)
						{
							// Get the vertex position.
							float x = UnsafeUtil.readFloat(pointer + i * format.getVertexSize() + format.getOffset(VertexFormatElement.POSITION));
							float y = UnsafeUtil.readFloat(pointer + i * format.getVertexSize() + format.getOffset(VertexFormatElement.POSITION) + 4);
							float z = UnsafeUtil.readFloat(pointer + i * format.getVertexSize() + format.getOffset(VertexFormatElement.POSITION) + 8);

							// Get the vertex alpha.
							int a = UnsafeUtil.readByte(pointer + i * format.getVertexSize() + format.getOffset(VertexFormatElement.COLOR) + 3) & 0xFF;

							// Add the vertex to the list if it's opaque.
							if (a >= 25)
							{
								vertices.add(new Vector3f(x, y, z));
							}
						}
					}
				};
			}
		};
	}
	
}
