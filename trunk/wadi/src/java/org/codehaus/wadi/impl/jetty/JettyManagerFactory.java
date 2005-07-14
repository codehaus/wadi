package org.codehaus.wadi.impl.jetty;

import java.net.InetSocketAddress;
import java.util.Map;

import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionManagerFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;

public class JettyManagerFactory implements SessionManagerFactory {

	public Object create(SessionPool sessionPool,AttributesFactory attributesFactory, ValuePool valuePool,SessionWrapperFactory sessionWrapperFactory,SessionIdFactory sessionIdFactory, Contextualiser contextualiser,Map sessionMap, Router router, Streamer streamer,boolean accessOnLoad, String clusterUri, String clusterName,String nodeName, HttpProxy httpProxy,InetSocketAddress httpAddress, int numBuckets) throws Exception {
		return new JettyManager(sessionPool,attributesFactory, valuePool,sessionWrapperFactory,sessionIdFactory, contextualiser, sessionMap, router, streamer, accessOnLoad, clusterUri, clusterName, nodeName, httpProxy, httpAddress, numBuckets);
	}

}
