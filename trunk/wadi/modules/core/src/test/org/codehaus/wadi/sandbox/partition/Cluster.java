package org.codehaus.wadi.sandbox.partition;

public interface Cluster {
	
	void start() throws Exception;
	void stop() throws Exception;
	
	String getNodeName(Object node);

}
