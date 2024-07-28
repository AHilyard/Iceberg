package com.anthonyhilyard.iceberg.renderer;

import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joml.Vector3f;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.services.Services;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class VertexCollector implements MultiBufferSource
{
	protected final Set<Vector3f> vertices = Sets.newHashSet();
	protected final Vector3f currentVertex = new Vector3f();
	protected int currentAlpha = 255;

	private static Boolean useSodiumVersion = null;

	protected VertexCollector()
	{
		super();
	}

	public static VertexCollector create()
	{
		if (useSodiumVersion == null)
		{
			try
			{
				// If Sodium 0.5.9+ is installed on Fabric, use the Sodium implementation.
				useSodiumVersion = Services.getPlatformHelper().getPlatformName().contentEquals("Fabric") && Services.getPlatformHelper().isModLoaded("sodium") && Services.getPlatformHelper().modVersionMeets("sodium", "0.5.9");

				// If Embeddium 1.0+ is installed on NeoForge, also use the Sodium implementation.
				useSodiumVersion |= Services.getPlatformHelper().getPlatformName().contentEquals("NeoForge") && Services.getPlatformHelper().isModLoaded("embeddium") && Services.getPlatformHelper().modVersionMeets("embeddium", "1.0.0");
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}

		if (useSodiumVersion != null && useSodiumVersion)
		{
			// Instantiate the Sodium implementation using our factory service.
			try
			{
				return Services.getBufferSourceFactory().createVertexCollector();
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return new VertexCollector();
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType)
	{
		return new VertexConsumer()
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
		};
	}

	public Set<Vector3f> getVertices()
	{
		return vertices;
	}
}