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
package org.codehaus.wadi.replication.storage.remoting;


import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import junit.framework.TestCase;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;


/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicReplicaStorageStubTest extends TestCase {

    private Address destination1;
    private Address destination2;
    private BasicReplicaStorageStub stub;
    private BaseMockDispatcher baseMockDispatcher;
    private ReplicaInfo info;
    private Address[] destinations;

    public void testMergeCreate() {
        final Address[] actualDestinations = new Address[destinations.length];
        final Serializable[] requests = new Serializable[destinations.length];
        baseMockDispatcher = new BaseMockDispatcher() {
            int idx = 0;
            public void register(ServiceEndpoint internalDispatcher) {
            }
            public void send(Address Address, Serializable request) throws MessageExchangeException {
                actualDestinations[idx] = Address;
                requests[idx++] = request;
            }
        };
        
        stub = new BasicReplicaStorageStub(
                        baseMockDispatcher,
                        destinations);

        stub.mergeCreate(new Object(), info);
        
        for (int i = 0; i < actualDestinations.length; i++) {
            assertSame(actualDestinations[i], destinations[i]);
            assertTrue(requests[i] instanceof CreateRequest);
        }
    }

    public void testMergeUpdate() {
        final Address[] actualDestinations = new Address[destinations.length];
        final Serializable[] requests = new Serializable[destinations.length];
        baseMockDispatcher = new BaseMockDispatcher() {
            int idx = 0;
            public void register(ServiceEndpoint internalDispatcher) {
            }
            public void send(Address Address, Serializable request) throws MessageExchangeException {
                actualDestinations[idx] = Address;
                requests[idx++] = request;
            }
        };
        
        stub = new BasicReplicaStorageStub(
                        baseMockDispatcher,
                        destinations);

        Object key = new Object();
        stub.mergeUpdate(key, info);
        
        for (int i = 0; i < actualDestinations.length; i++) {
            assertSame(actualDestinations[i], destinations[i]);
            assertTrue(requests[i] instanceof UpdateRequest);
        }
    }

    public void testMergeDestroy() {
        final Address[] actualDestinations = new Address[destinations.length];
        final Serializable[] requests = new Serializable[destinations.length];
        baseMockDispatcher = new BaseMockDispatcher() {
            int idx = 0;
            public void register(ServiceEndpoint internalDispatcher) {
            }
            public void send(Address Address, Serializable request) throws MessageExchangeException {
                actualDestinations[idx] = Address;
                requests[idx++] = request;
            }
        };
        
        stub = new BasicReplicaStorageStub(
                        baseMockDispatcher,
                        destinations);

        Object key = new Object();
        stub.mergeDestroy(key);
        
        for (int i = 0; i < actualDestinations.length; i++) {
            assertSame(actualDestinations[i], destinations[i]);
            assertTrue(requests[i] instanceof DestroyRequest);
        }
    }

    public void testRetrieveReplicaInfo() {
        final String payload = "payload";
        final Address[] actualDestinations = new Address[1];
        final Serializable[] requests = new Serializable[1];
        baseMockDispatcher = new BaseMockDispatcher() {
            int idx = 0;
            public void register(ServiceEndpoint internalDispatcher) {
            }
            public Envelope exchangeSend(Address to, Serializable request, long timeout) {
                actualDestinations[idx] = to;
                requests[idx++] = request;
                return new BaseMockEnvelope() {
                    public Serializable getPayload() {
                        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
                        try {
                            ObjectOutputStream os = new ObjectOutputStream(
                                    memOut);
                            os.writeObject(payload);
                            os.flush();
                            os.close();
                        } catch (Exception e) {
                            fail();
                        }
                        return new ReplicaStorageResult(
                                new ReplicaInfo((NodeInfo) null, null, memOut.toByteArray()));
                    }
                };
            }
        };
        
        stub = new BasicReplicaStorageStub(
                        baseMockDispatcher,
                        new Address[] {destination2, destination1});

        Object key = new Object();
        ReplicaInfo retrievedReplicaInfo = stub.retrieveReplicaInfo(key);
        
        assertSame(actualDestinations[0], destination2);
        assertTrue(requests[0] instanceof ReplicaStorageRequest);
        assertEquals(payload, retrievedReplicaInfo.getReplica());
    }
    
    protected void setUp() throws Exception {
        destination1 = new BaseMockAddress();
        destination2 = new BaseMockAddress();
        info = new ReplicaInfo((NodeInfo) null, null, null);
        destinations = new Address[] {destination1, destination2};
    }
}
