/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.activecluster;

import java.net.URI;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.transport.TransportServer;
import org.apache.activemq.transport.tcp.TcpTransportFactory;

/**
 * 
 * @version $Revision: $
 */
public class ACTestUtil {
    public static final String CLUSTER_NAME = ActiveClusterCluster.class.getPackage().getName() + ".TEST";
    public static final long CLUSTER_INACTIVE_TIME = 5000;

    public static final String CLUSTER_URI_TCP = "tcp://localhost:61618";
    public static final String CLUSTER_URI_PEER = "peer://" + ActiveClusterCluster.class.getPackage().getName()
            + ".TEST-" + Math.random() + "?persistent=false&useJmx=false";
    public static final String CLUSTER_URI_VM = "vm://localhost?broker.persistent=false&broker.useJmx=false";

    private BrokerService service; 
    
	public void startTCPService() throws Exception {
        service = new BrokerService();
        TcpTransportFactory tcpTransportFactory = new TcpTransportFactory();
        TransportServer server = tcpTransportFactory.doBind("name", new URI(CLUSTER_URI_TCP));
        service.addConnector(server);
        service.setUseJmx(false);
        service.start();
	}

    public void stopTCPService() throws Exception {
        service.stop();
        Thread.sleep(1000);
    }

}

