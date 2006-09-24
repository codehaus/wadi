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
package org.codehaus.wadi.partition;

import java.io.Serializable;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;

/**
 * 
 * @version $Revision: 1538 $
 */
public class UnknownPartitionBalancingInfo extends PartitionBalancingInfo {

    public UnknownPartitionBalancingInfo(Peer definingPeer, int nbPartitions) {
        super(definingPeer, buildUnownedPartitions(nbPartitions));
    }

    private static PartitionBalancingInfo buildUnownedPartitions(int nbPartitions) {
        PartitionInfo[] partitionInfos = new PartitionInfo[nbPartitions];
        for (int i = 0; i < partitionInfos.length; i++) {
            partitionInfos[i] = new PartitionInfo(i, new UnknownPeer());
        }
        return new PartitionBalancingInfo(partitionInfos);
    }
    
    public static class UnknownPeer implements Peer, Serializable {

        public Address getAddress() {
            throw new UnsupportedOperationException();
        }

        public String getName() {
            throw new UnsupportedOperationException();
        }

        public PeerInfo getPeerInfo() {
            throw new UnsupportedOperationException();
        }
        
    }
}
