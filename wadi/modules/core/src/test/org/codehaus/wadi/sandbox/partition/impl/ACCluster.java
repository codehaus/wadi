/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.sandbox.partition.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import org.activecluster.ClusterEvent;
import org.activecluster.ClusterFactory;
import org.activecluster.ClusterListener;
import org.activecluster.Node;
import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
//import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.partition.Cluster;

public class ACCluster implements Cluster {

	protected final Log _log = LogFactory.getLog(getClass());

	//protected final String _clusterUri="tcp://smilodon:61616";
	protected final String _clusterUri="peer://org.codehaus.wadi?persistent=false";
    protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
    protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
    protected final ClusterFactory _clusterFactory=new DefaultClusterFactory(_connectionFactory);
	protected final org.activecluster.Cluster _cluster;
	protected final long _timeout=30*1000L;

	public ACCluster(String nodeName) throws Exception {
		//System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName());
		_cluster=_clusterFactory.createCluster(_clusterName);
		Map state=new HashMap();
		state.put("nodeName", nodeName);
		_cluster.getLocalNode().setState(state);
		_cluster.addClusterListener(new ClusterListener() {

			public void onNodeAdd(ClusterEvent arg0) {
				_log.info("onNodeAdd: "+getNodeName(arg0.getNode()));
			}

			public void onNodeUpdate(ClusterEvent arg0) {
				_log.info("onNodeUpdate: "+getNodeName(arg0.getNode()));
			}

			public void onNodeRemoved(ClusterEvent arg0) {
				_log.info("onNodeRemoved: "+getNodeName(arg0.getNode()));
			}

			public void onNodeFailed(ClusterEvent arg0) {
				_log.info("onNodeFailed: "+getNodeName(arg0.getNode()));
			}

			public void onCoordinatorChanged(ClusterEvent arg0) {
				_log.info("onCoordinatorChanged: "+getNodeName(arg0.getNode()));
			}
		});
	}

	public String getNodeName(Object node) {
		return (String)((Node)node).getState().get("nodeName");
	}

	public void start() throws JMSException {
		_log.info("starting...");
		_cluster.start();
		_log.info("...started");
	}

	public void stop() {

	}

	public static void main(String[] args) throws Exception {
		Cluster cluster=new ACCluster(args[0]);
		cluster.start();
		Thread.sleep(100*1000);
		cluster.stop();
	}
}
