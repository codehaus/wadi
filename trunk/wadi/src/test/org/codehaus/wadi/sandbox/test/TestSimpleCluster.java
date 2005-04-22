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
package org.codehaus.wadi.sandbox.test;

import junit.framework.TestCase;

import org.activecluster.Cluster;
import org.activecluster.impl.ActiveMQClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.impl.Utils;


public class TestSimpleCluster extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());

	public void testCluster() throws Exception {

        ActiveMQConnectionFactory connectionFactory=(ActiveMQConnectionFactory)Utils.getConnectionFactory();
        ActiveMQClusterFactory clusterFactory=new ActiveMQClusterFactory(connectionFactory);
        String clusterName="ORG.CODEHAUS.WADI.TEST.CLUSTER";
        Cluster cluster0=clusterFactory.createCluster(clusterName);
        Cluster cluster1=clusterFactory.createCluster(clusterName);

        cluster0.waitForClusterToComplete(1, 6000);
        
        
        Thread.sleep(6000);
        
        _log.info("stopping");
        cluster1.stop();
        cluster0.stop();
    }
}
