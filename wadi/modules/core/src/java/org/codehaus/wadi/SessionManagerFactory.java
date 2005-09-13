package org.codehaus.wadi;

import java.net.InetSocketAddress;
import java.util.Map;


public interface SessionManagerFactory {

	public abstract Object create(SessionPool sessionPool,
			AttributesFactory attributesFactory, ValuePool valuePool,
			SessionWrapperFactory sessionWrapperFactory,
			SessionIdFactory sessionIdFactory, Contextualiser contextualiser,
			Map sessionMap, Router router, Streamer streamer,
			boolean accessOnLoad, String clusterUri, String clusterName,
			String nodeName, HttpProxy httpProxy,
			InetSocketAddress httpAddress, int numBuckets) throws Exception;

}