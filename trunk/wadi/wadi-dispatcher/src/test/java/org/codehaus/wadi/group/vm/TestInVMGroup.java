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
package org.codehaus.wadi.group.vm;

import java.util.HashMap;
import java.util.Map;
import org.codehaus.wadi.group.AbstractTestGroup;
import org.codehaus.wadi.group.Dispatcher;

/**
 * 
 * @version $Revision: 1603 $
 */
public class TestInVMGroup extends AbstractTestGroup {
    private Map clusterNameToCluster = new HashMap();
    
    public TestInVMGroup(String name) {
        super(name);
    }
    
    public DispatcherFactory getDispatcherFactory() {
        return new InVMDispatcherFactory();
    }
    
    private class InVMDispatcherFactory implements DispatcherFactory {
        public Dispatcher create(String clusterName, String peerName, long inactiveTime) throws Exception {
            VMBroker cluster = (VMBroker) clusterNameToCluster.get(clusterName);
            if (null == cluster) {
                cluster = new VMBroker(clusterName);
                clusterNameToCluster.put(clusterName, cluster);
            }
            return new VMDispatcher(cluster, peerName, inactiveTime);
        }
    }
}
