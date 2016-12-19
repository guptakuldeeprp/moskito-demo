package com.zycus.monitoring.moskito.client;

import java.util.List;

import net.anotheria.moskito.core.stats.TimeUnit;
import net.anotheria.moskito.webui.producers.api.ProducerAO;
import net.anotheria.moskito.webui.producers.api.ProducerAPI;
import net.anotheria.moskito.webui.util.RemoteInstance;

import com.zycus.monitoring.moskito.connector.MoskitoConnector;

public class RemoteConnectorClient {

	public static void main(String[] args) throws Exception {
		RemoteInstance ri = new RemoteInstance();
		ri.setHost("192.168.4.92");
		ri.setPort(11111);
		ri.setName("burgershop");

		ProducerAPI producerApi = MoskitoConnector.getProducerAPI(ri);
		List<ProducerAO> producers = producerApi.getAllProducers("1m", TimeUnit.MILLISECONDS);

		for (ProducerAO producer : producers) {
			System.out.println(producer);
		}
		
	}

}
