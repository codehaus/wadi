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
package org.codehaus.wadi.activecluster;

public class ActiveClusterClusterPeer extends ActiveClusterPeer {

    protected static final String _prefix="<"+Utils.basename(ActiveClusterClusterPeer.class)+": ";
    protected static final String _suffix=">";
    
    public ActiveClusterClusterPeer(ActiveClusterCluster cluster) {
        super(cluster);
    }
    
    // 'java.lang.Object' API
    
    public String toString() {
        return _prefix+getName()+"/"+_acDestination+_suffix;
    }    
    
    // 'org.codehaus.wadi.group.Peer' API
    
    public String getName() {
        return _cluster.getName();
    }

}