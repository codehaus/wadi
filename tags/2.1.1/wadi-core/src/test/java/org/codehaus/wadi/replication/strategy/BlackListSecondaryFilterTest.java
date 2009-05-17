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

package org.codehaus.wadi.replication.strategy;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.wadi.group.Peer;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BlackListSecondaryFilterTest extends RMockTestCase {

    public void testFilter() throws Exception {
        Peer blacklisted = (Peer) mock(Peer.class);
        Peer peer = (Peer) mock(Peer.class);
        
        List<Peer> peersToFilter = new ArrayList<Peer>();
        peersToFilter.add(peer);
        peersToFilter.add(blacklisted);
        
        BlackListSecondaryFilter filter = new BlackListSecondaryFilter(blacklisted);
        List<Peer> filteredPeer = filter.filter(peersToFilter);
        assertEquals(1, filteredPeer.size());
        assertTrue(filteredPeer.contains(peer));
    }
    
}
