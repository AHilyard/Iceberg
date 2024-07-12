package com.anthonyhilyard.iceberg.forge.services;

import com.anthonyhilyard.iceberg.renderer.CheckedBufferSource;
import com.anthonyhilyard.iceberg.renderer.VertexCollector;
import com.anthonyhilyard.iceberg.services.IBufferSourceFactory;

import net.minecraft.client.renderer.MultiBufferSource;

public class ForgeBufferSourceFactory implements IBufferSourceFactory
{
	@Override
	public CheckedBufferSource createCheckedBufferSource(MultiBufferSource bufferSource)
	{
		throw new UnsupportedOperationException("This version of Iceberg is incompatible with Rubidium/Embeddium.");
	}

	@Override
	public VertexCollector createVertexCollector()
	{
		throw new UnsupportedOperationException("This version of Iceberg is incompatible with Rubidium/Embeddium.");
	}
	
}
