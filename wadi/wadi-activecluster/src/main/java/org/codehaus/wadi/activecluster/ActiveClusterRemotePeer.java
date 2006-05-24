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
package org.codehaus.wadi.activecluster;

/**
 * 
 * @version $Revision: 1603 $
 */
class ActiveClusterRemotePeer extends ActiveClusterPeer {
    
    protected static final String _prefix="<"+Utils.basename(ActiveClusterRemotePeer.class)+": ";
    protected static final String _suffix=">";
    
    public ActiveClusterRemotePeer(ActiveClusterCluster cluster, javax.jms.Destination acDestination) {
        super(cluster);
        init(acDestination);
    }

    // 'java.lang.Object' API

    public String toString() {
        return _prefix+getName()+_suffix;
    }

    public String getName() {
        String name=super.getName();
        return (name==null)?"<unknown>":name;
    }
    
}
