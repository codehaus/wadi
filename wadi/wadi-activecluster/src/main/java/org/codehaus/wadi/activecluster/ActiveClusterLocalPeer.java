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

import java.util.Map;

import javax.jms.JMSException;

import org.apache.activecluster.LocalNode;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;

/**
 * 
 * @version $Revision: 1603 $
 */
class ActiveClusterLocalPeer extends ActiveClusterPeer implements LocalPeer {
    
    protected static final String _prefix="<"+Utils.basename(ActiveClusterLocalPeer.class)+": ";
    protected static final String _suffix=">";
    
    public ActiveClusterLocalPeer(ActiveClusterCluster cluster) {
        super(cluster);
    }

    // 'java.lang.Object' API

    public String toString() {
        return _prefix+getName()+_suffix;
    }

    public void setState(Map state) throws MessageExchangeException {
        super.setState(state);
        org.apache.activecluster.Cluster acCluster=_cluster.getACCluster();
        if (acCluster!=null) {
            try {
                acCluster.getLocalNode().setState(state);
            } catch (JMSException e) {
                throw new MessageExchangeException(e);
            }
        }
    }
    
}
