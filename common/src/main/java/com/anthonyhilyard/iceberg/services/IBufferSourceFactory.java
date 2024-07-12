package com.anthonyhilyard.iceberg.services;

import com.anthonyhilyard.iceberg.renderer.CheckedBufferSource;
import com.anthonyhilyard.iceberg.renderer.VertexCollector;

import net.minecraft.client.renderer.MultiBufferSource;

public interface IBufferSourceFactory
{
	CheckedBufferSource createCheckedBufferSource(MultiBufferSource bufferSource);

	VertexCollector createVertexCollector();
}
