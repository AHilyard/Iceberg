package com.anthonyhilyard.iceberg.renderer;

import static com.anthonyhilyard.iceberg.renderer.CheckedBufferSource.useSodiumVersion;

import java.util.Set;

import org.joml.Vector3f;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class VertexCollector implements MultiBufferSource
{
	protected final Set<Vector3f> vertices = Sets.newHashSet();
	protected final Vector3f currentVertex = new Vector3f();
	protected int currentAlpha = 255;
	protected int defaultAlpha = 255;

	protected VertexCollector()
	{
		super();
	}

	public static VertexCollector create()
	{
		if (useSodiumVersion)
		{
			return new VertexCollectorSodium();
		}
		return new VertexCollector();
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType)
	{
		return new VertexConsumer()
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
		};
	}

	public Set<Vector3f> getVertices()
	{
		return vertices;
	}
}
