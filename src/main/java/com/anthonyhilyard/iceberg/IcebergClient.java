package com.anthonyhilyard.iceberg;

import com.anthonyhilyard.iceberg.network.IcebergNetworkProtocol;

import net.fabricmc.api.ClientModInitializer;

public class IcebergClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		IcebergNetworkProtocol.registerHandlers();
	}
}
