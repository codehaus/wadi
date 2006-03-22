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
package org.codehaus.wadi.replication.strategy;

import org.codehaus.wadi.replication.common.NodeInfo;

import junit.framework.TestCase;

public class RoundRobinBackingStrategyTest extends TestCase {
    private RoundRobinBackingStrategy strategy;
    private NodeInfo node1;
    private NodeInfo node2;
    private NodeInfo node3;
    private NodeInfo node4;

    public void testElectSecondaries() {
        strategy.addSecondaries(new NodeInfo[] {node1, node2, node3, node4});
        
        NodeInfo[] actualSecondaries = strategy.electSecondaries(null);
        assertEquals(2, actualSecondaries.length);
        assertEquals(node1, actualSecondaries[0]);
        assertEquals(node2, actualSecondaries[1]);
        
        actualSecondaries = strategy.electSecondaries(null);
        assertEquals(2, actualSecondaries.length);
        assertEquals(node3, actualSecondaries[0]);
        assertEquals(node4, actualSecondaries[1]);

        strategy.removeSecondary(node2);

        actualSecondaries = strategy.electSecondaries(null);
        assertEquals(2, actualSecondaries.length);
        assertEquals(node1, actualSecondaries[0]);
        assertEquals(node3, actualSecondaries[1]);

        actualSecondaries = strategy.electSecondaries(null);
        assertEquals(2, actualSecondaries.length);
        assertEquals(node4, actualSecondaries[0]);
        assertEquals(node1, actualSecondaries[1]);
   }

    protected void setUp() throws Exception {
        strategy = new RoundRobinBackingStrategy(2);
        node1 = new NodeInfo("node1");
        node2 = new NodeInfo("node2");
        node3 = new NodeInfo("node3");
        node4 = new NodeInfo("node4");
    }
}
