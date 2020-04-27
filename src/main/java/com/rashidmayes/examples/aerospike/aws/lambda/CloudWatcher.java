package com.rashidmayes.examples.aerospike.aws.lambda;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Info;
import com.aerospike.client.cluster.Node;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class CloudWatcher implements RequestHandler<Object, String> {

    @Override
    public String handleRequest(Object input, Context context) {

        String host = System.getenv("aerospike_host");
        int port = Integer.parseInt(System.getenv("aerospike_port"));
        String namespace = System.getenv("metrics_namespace");
        final AmazonCloudWatch cloudWatch =
    		    AmazonCloudWatchClientBuilder.defaultClient();
        collectStats(host, port, namespace, cloudWatch);
        
        
        return String.format("Stats collected from %s:%d and saved to %s", host, port, namespace);
    }
    
    
    public void collectStats(String host, int port, String statsNamespace, AmazonCloudWatch cloudWatch) {
    	
    	
    	try (AerospikeClient client = new AerospikeClient(host,port)) {
    		
    		Node node = client.getNodes()[0];
    		
    		Map<String,String> map = Info.request(null, node);
    		/*
    		map.forEach((key,value) -> {
    			System.out.println(key + "\t" + value);    			
    		});*/
    		
    		String statistics = map.get("statistics");
    		publish(statsNamespace, "NODE", node.getName(), statistics, cloudWatch);
    		

    		for ( String namespace : Info.request(null, node, "namespaces").split(";") ) {
    			
    			statistics = Info.request(null, node, "namespace/" + namespace);
    			publish(statsNamespace, "NAMESPACE", namespace, statistics, cloudWatch);
    		}
    	}
    }
    
    
    public void publish(String namespace, String dimensionName, String dimensionValue, String statString, AmazonCloudWatch cloudWatch) {

		int chunkSize = 20;
		final AtomicInteger counter = new AtomicInteger();
		String[] tuple;
		
		PutMetricDataRequest request = new PutMetricDataRequest()
    		    .withNamespace(namespace);
		
		Dimension dimension = new Dimension()
    		    .withName(dimensionName)
    		    .withValue(dimensionValue);
		
		List<MetricDatum> metrics = new ArrayList<MetricDatum>();
		StandardUnit units;
		for ( String statAndValue : statString.split(";") ) {
			tuple = statAndValue.split("=");
			if ( tuple[1].chars().allMatch(Character::isDigit) ) {
				
				if ( tuple[0].indexOf("kbytes") != -1 ) {
					units = StandardUnit.Kilobytes;
				} else if ( tuple[0].indexOf("bytes") != -1 ) {
    				units = StandardUnit.Bytes;
				} else {
    				units = StandardUnit.None;
				}
				
				metrics.add(new MetricDatum()
						.withMetricName(tuple[0])
		    		    .withUnit(units)
		    		    .withValue(Double.parseDouble(tuple[1]))
		    		    .withDimensions(dimension)					
				);
			}
		}
		
		
		metrics.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize)).forEach((num, sublist) -> {
			cloudWatch.putMetricData(request.withMetricData(sublist));
		});
    }
}