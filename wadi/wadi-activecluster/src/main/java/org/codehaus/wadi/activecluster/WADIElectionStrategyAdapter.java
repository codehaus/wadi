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

import javax.jms.JMSException;

import org.apache.activecluster.Node;
import org.apache.activecluster.election.ElectionStrategy;
import org.codehaus.wadi.group.Cluster;

/**
 * 
 * @version $Revision: 1603 $
 */
class WADIElectionStrategyAdapter implements ElectionStrategy {
    private final org.codehaus.wadi.group.ElectionStrategy adaptee;
    private final Cluster cluster;
    
    public WADIElectionStrategyAdapter(org.codehaus.wadi.group.ElectionStrategy adaptee, Cluster cluster) {
        this.adaptee = adaptee;
        this.cluster = cluster;
    }

    public Node doElection(org.apache.activecluster.Cluster acCluster) throws JMSException {
        org.codehaus.wadi.group.Peer electedNode = adaptee.doElection(cluster);
        
        if (null == electedNode) {
            return null;
        } else if (electedNode instanceof ACNodeAdapter) {
            return ((ACNodeAdapter) electedNode).getAdaptee();
        } else if (electedNode instanceof ACLocalNodeAdapter) {
            return ((ACLocalNodeAdapter) electedNode).getAdaptee();
        }

        throw new IllegalStateException("Elected node is not a " + 
                ACNodeAdapter.class.getName() +
                " or a " +
                ACLocalNodeAdapter.class.getName() + ". Was:" +
                electedNode.getClass().getName());            
    }
}
