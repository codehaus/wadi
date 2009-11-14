/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.location.statemanager;

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.location.partitionmanager.Partition;
import org.codehaus.wadi.location.partitionmanager.PartitionManager;
import org.codehaus.wadi.location.session.ReleaseEntryRequest;
import org.codehaus.wadi.location.session.ReleaseEntryResponse;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.servicespace.ServiceSpace;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class SimpleStateManagerTest extends RMockTestCase {

    private long inactiveTime;
    private ServiceSpace serviceSpace;
    private Dispatcher dispatcher;
    private Cluster cluster;
    private PartitionManager partitionManager;

    @Override
    protected void setUp() throws Exception {
        inactiveTime = 10;
        
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        dispatcher = serviceSpace.getDispatcher();
        cluster = dispatcher.getCluster();
        cluster.getLocalPeer();
        
        partitionManager = (PartitionManager) mock(PartitionManager.class);
    }
    
    public void testOfferEmigrant() throws Exception {
        Motable emotable = (Motable) mock(Motable.class);
        emotable.getId();
        String key = "key";
        modify().returnValue(key);
        
        Partition partition = partitionManager.getPartition(key);
        partition.exchange(null, inactiveTime);
        Envelope responseEnvelope = (Envelope) mock(Envelope.class);
        modify().args(is.instanceOf(ReleaseEntryRequest.class), is.AS_RECORDED).returnValue(responseEnvelope);
        ReleaseEntryResponse response = new ReleaseEntryResponse(true);
        responseEnvelope.getPayload();
        modify().returnValue(response);
        
        startVerification();
        
        SimpleStateManager stateManager = new SimpleStateManager(serviceSpace,
                partitionManager,
                inactiveTime);
        boolean success = stateManager.offerEmigrant(emotable, new ReplicaInfo());
        assertTrue(success);
    }

}
