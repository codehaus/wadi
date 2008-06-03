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

package org.codehaus.wadi.servicespace.admin.commands;

import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.servicespace.InvocationResult;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CountingEnvelopeInfoCombinerTest extends RMockTestCase {

    public void testBasicCombination() throws Exception {
        Peer peer = (Peer) mock(Peer.class);
        
        startVerification();
        
        Collection<InvocationResult> results = new ArrayList<InvocationResult>();
        results.add(new InvocationResult((Object) null));
        CountingEnvelopeInfo countInfo = new CountingEnvelopeInfo(peer, 1, 2);
        InvocationResult result = new InvocationResult(countInfo);
        results.add(result);
        
        InvocationResult combinebResults = CountingEnvelopeInfoCombiner.COMBINER.combine(results);
        Collection actualsCountInfo = (Collection) combinebResults.getResult();
        assertEquals(1, actualsCountInfo.size());
        assertTrue(actualsCountInfo.contains(countInfo));
    }
    
}
