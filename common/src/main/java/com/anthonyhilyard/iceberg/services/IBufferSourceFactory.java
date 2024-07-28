package com.anthonyhilyard.iceberg.services;

import com.anthonyhilyard.iceberg.renderer.CheckedBufferSource;
import com.anthonyhilyard.iceberg.renderer.VertexCollector;

public interface IBufferSourceFactory
{
	CheckedBufferSource createCheckedBufferSource(Object bufferSource);

	VertexCollector createVertexCollector();
}
