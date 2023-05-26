package com.anthonyhilyard.iceberg.renderer;

import java.util.Set;

import com.mojang.math.Vector3f;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import com.anthonyhilyard.iceberg.Loader;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fml.ModList;

public class VertexCollector implements MultiBufferSource
{
	protected final Set<Vector3f> vertices = Sets.newHashSet();
	protected final Vector3f currentVertex = new Vector3f();
	protected int currentAlpha = 255;
	protected int defaultAlpha = 255;

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
				// Check if Rubidium 0.6.4 is installed using Forge API.
				useSodiumVersion = ModList.get().isLoaded("rubidium") && ModList.get().getModContainerById("rubidium").get().getModInfo().getVersion().equals(new DefaultArtifactVersion("0.6.4"));
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
				return (VertexCollector) Class.forName("com.anthonyhilyard.iceberg.renderer.VertexCollectorSodium").getDeclaredConstructor().newInstance();
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
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
					vertices.add(currentVertex.copy());
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
