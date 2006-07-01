/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.jgroups;

import org.codehaus.wadi.group.AbstractTestGroup;
import org.codehaus.wadi.group.Dispatcher;

public class TestJGGroup extends AbstractTestGroup {

    public TestJGGroup(String name) {
        super(name);
    }

    public DispatcherFactory getDispatcherFactory() {
        return new DispatcherFactory() {
            public Dispatcher create(String clusterName, String peerName, long inactiveTime) throws Exception {return new JGroupsDispatcher(clusterName, peerName, inactiveTime, JGroupsCluster.TEST_CLUSTER_CONFIG);}
        };
    }

}
