package org.codehaus.wadi;

import java.net.InetSocketAddress;
import java.util.Map;

import javax.jms.JMSException;

import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.impl.ActiveClusterDispatcher;
import org.codehaus.wadi.io.Server;

public interface ClusteredContextualiserConfig extends DistributableContextualiserConfig {

	ExtendedCluster getCluster();
    Server getServer();
    String getNodeName();
    HttpProxy getHttpProxy();
    InetSocketAddress getHttpAddress();

    Object getDistributedState(Object key);
    Object putDistributedState(Object key, Object value);
    Object removeDistributedState(Object key);
    void distributeState() throws JMSException;

    Map getDistributedState();
    long getInactiveTime();
    int getNumBuckets();
    ActiveClusterDispatcher getDispatcher();
    DIndex getDIndex();

}
