package com.anthonyhilyard.iceberg.renderer;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.services.Services;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class CheckedBufferSource implements MultiBufferSource
{
	protected boolean hasRendered = false;
	protected final MultiBufferSource bufferSource;

	private static Boolean useSodiumVersion = null;

	protected CheckedBufferSource(MultiBufferSource bufferSource)
	{
		this.bufferSource = bufferSource;
	}

	public static CheckedBufferSource create(MultiBufferSource bufferSource)
	{
		if (useSodiumVersion == null)
		{
			try
			{
				// If Sodium 0.5.9+ is installed on Fabric, use the Sodium implementation.
				useSodiumVersion = Services.PLATFORM.getPlatformName().contentEquals("Fabric") && Services.PLATFORM.isModLoaded("sodium") && Services.PLATFORM.modVersionMeets("sodium", "0.5.9");

				// If Embeddium 1.0+ is installed on NeoForge, also use the Sodium implementation.
				useSodiumVersion |= Services.PLATFORM.getPlatformName().contentEquals("NeoForge") && Services.PLATFORM.isModLoaded("embeddium") && Services.PLATFORM.modVersionMeets("embeddium", "1.0.0");
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
				return Services.BUFFER_SOURCE_FACTORY.createCheckedBufferSource(bufferSource);
			}
			catch (Exception e)
			{
				Iceberg.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}

		return new CheckedBufferSource(bufferSource);
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType)
	{
		final VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
		VertexConsumer vertexConsumerWrap = new VertexConsumer()
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
		};

		return vertexConsumerWrap;
	}
	
	public boolean hasRendered()
	{
		return hasRendered;
	}

	public void reset()
	{
		hasRendered = false;
	}
}
