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
package org.codehaus.wadi.servicespace.basic;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.DispatcherContext;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.BasicDispatcherContext;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AbstractServiceSpaceTestCase extends RMockTestCase {

    protected LocalPeer localPeer;
    protected Peer remote1;
    protected Address address1;
    protected Peer remote2;
    protected Address address2;
    protected Map remotePeers;
    protected Cluster cluster;
    protected Dispatcher dispatcher;
    protected ServiceSpaceName serviceSpaceName;
    protected ServiceSpace serviceSpace;

    protected void setUp() throws Exception {
        setUpPeers();
        setUpCluster();
        setUpDispatcher();
        setUpServiceSpace();
    }
    
    private void setUpPeers() {
        localPeer = (LocalPeer) mock(LocalPeer.class);
        
        remote1 = (Peer) mock(Peer.class);
        remote1.getAddress();
        address1 = (Address) mock(Address.class);
        modify().multiplicity(expect.from(0)).returnValue(address1);
        
        remote2 = (Peer) mock(Peer.class);
        remote2.getAddress();
        address2 = (Address) mock(Address.class);
        modify().multiplicity(expect.from(0)).returnValue(address2);
        
        remotePeers = new HashMap();
        remotePeers.put("remote1", remote1);
        remotePeers.put("remote2", remote2);
    }

    private void setUpCluster() {
        cluster = (Cluster) mock(Cluster.class);
        cluster.getLocalPeer();
        modify().multiplicity(expect.from(0)).returnValue(localPeer);
        cluster.getRemotePeers();
        modify().multiplicity(expect.from(0)).returnValue(remotePeers);
    }

    private void setUpDispatcher() {
        dispatcher = (Dispatcher) mock(Dispatcher.class);
        dispatcher.getCluster();
        modify().multiplicity(expect.from(0)).returnValue(cluster);
        
        dispatcher.getContext();
        modify().multiplicity(expect.from(0)).returnValue(new BasicDispatcherContext());
    }

    private void setUpServiceSpace() throws URISyntaxException {
        serviceSpaceName = new ServiceSpaceName(new URI("space"));

        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        serviceSpace.getDispatcher();
        modify().multiplicity(expect.from(0)).returnValue(dispatcher);
        
        serviceSpace.getServiceSpaceName();
        modify().multiplicity(expect.from(0)).returnValue(serviceSpaceName);
        
        serviceSpace.getLocalPeer();
        modify().multiplicity(expect.from(0)).returnValue(localPeer);
    }
    
}
