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
package org.codehaus.wadi.location.partitionmanager.local;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractLocalPartitionActionTest extends RMockTestCase {
    protected static final Log log = LogFactory.getLog(AbstractLocalPartitionActionTest.class);
    
    protected Dispatcher dispatcher;
    protected Cluster cluster;
    protected LocalPeer localPeer;
    protected Map nameToLocation;
    protected Peer peer;
    protected Address peerAddress;
    protected Envelope envelope;

    protected void setUp() throws Exception {
        dispatcher = (Dispatcher) mock(Dispatcher.class);
        cluster = dispatcher.getCluster();
        modify().multiplicity(expect.atLeast(0));
        localPeer = cluster.getLocalPeer();
        modify().multiplicity(expect.atLeast(0));
        
        nameToLocation = new HashMap();
        peer = (Peer) mock(Peer.class);
        peerAddress = peer.getAddress();
        modify().multiplicity(expect.atLeast(0));
        
        envelope = (Envelope) mock(Envelope.class);
    }
    
}
