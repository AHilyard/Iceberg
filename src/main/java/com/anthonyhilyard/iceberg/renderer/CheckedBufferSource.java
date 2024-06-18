package com.anthonyhilyard.iceberg.renderer;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.anthonyhilyard.iceberg.Loader;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
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
				// If Sodium 0.5.0+ is installed, use the Sodium implementation.
				useSodiumVersion = FabricLoader.getInstance().isModLoaded("sodium") && VersionPredicate.parse(">=0.5.0").test(FabricLoader.getInstance().getModContainer("sodium").get().getMetadata().getVersion());
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}

		if (useSodiumVersion != null && useSodiumVersion)
		{
			// Instantiate the Sodium implementation using reflection.
			try
			{
				return (CheckedBufferSource) Class.forName("com.anthonyhilyard.iceberg.renderer.CheckedBufferSourceSodium").getDeclaredConstructor(MultiBufferSource.class).newInstance(bufferSource);
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
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
