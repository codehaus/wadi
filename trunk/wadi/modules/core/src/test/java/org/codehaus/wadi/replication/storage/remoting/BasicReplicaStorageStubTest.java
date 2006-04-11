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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import junit.framework.TestCase;

import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;


/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicReplicaStorageStubTest extends TestCase {

    private Destination destination1;
    private Destination destination2;
    private BasicReplicaStorageStub stub;
    private BaseMockDispatcher baseMockDispatcher;
    private ReplicaInfo info;
    private Destination[] destinations;

    public void testMergeCreate() {
        final Destination[] actualDestinations = new Destination[destinations.length];
        final Serializable[] requests = new Serializable[destinations.length];
        baseMockDispatcher = new BaseMockDispatcher() {
            int idx = 0;
            public void register(Class type, long timeout) {
                assertSame(ReplicaStorageResult.class, type);
            }
            public void send(Destination destination, Serializable request) throws Exception {
                actualDestinations[idx] = destination;
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
        final Destination[] actualDestinations = new Destination[destinations.length];
        final Serializable[] requests = new Serializable[destinations.length];
        baseMockDispatcher = new BaseMockDispatcher() {
            int idx = 0;
            public void register(Class type, long timeout) {
                assertSame(ReplicaStorageResult.class, type);
            }
            public void send(Destination destination, Serializable request) throws Exception {
                actualDestinations[idx] = destination;
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
        final Destination[] actualDestinations = new Destination[destinations.length];
        final Serializable[] requests = new Serializable[destinations.length];
        baseMockDispatcher = new BaseMockDispatcher() {
            int idx = 0;
            public void register(Class type, long timeout) {
                assertSame(ReplicaStorageResult.class, type);
            }
            public void send(Destination destination, Serializable request) throws Exception {
                actualDestinations[idx] = destination;
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
        final Destination[] actualDestinations = new Destination[1];
        final Serializable[] requests = new Serializable[1];
        baseMockDispatcher = new BaseMockDispatcher() {
            int idx = 0;
            public Destination getLocalDestination() {
                return destination1;
            }
            
            public void register(Class type, long timeout) {
                assertSame(ReplicaStorageResult.class, type);
            }
            public ObjectMessage exchangeSend(Destination from, Destination to, Serializable request, long timeout) {
                assertSame(destination1, from);
                actualDestinations[idx] = to;
                requests[idx++] = request;
                return new BaseMockObjectMessage() {
                    public Serializable getObject() throws JMSException {
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
                        new Destination[] {destination2, destination1});

        Object key = new Object();
        ReplicaInfo retrievedReplicaInfo = stub.retrieveReplicaInfo(key);
        
        assertSame(actualDestinations[0], destination2);
        assertTrue(requests[0] instanceof ReplicaStorageRequest);
        assertEquals(payload, retrievedReplicaInfo.getReplica());
    }
    
    protected void setUp() throws Exception {
        destination1 = new BaseMockDestination();
        destination2 = new BaseMockDestination();
        info = new ReplicaInfo((NodeInfo) null, null, null);
        destinations = new Destination[] {destination1, destination2};
    }
}
