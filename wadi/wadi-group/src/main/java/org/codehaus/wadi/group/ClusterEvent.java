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
package org.codehaus.wadi.group;


/**
 * 
 * @version $Revision: 1603 $
 */
public class ClusterEvent {
    public static final Type PEER_ADDED = new Type("PEER_ADDED");
    public static final Type PEER_UPDATED = new Type("PEER_UPDATED");
    public static final Type PEER_REMOVED = new Type("PEER_REMOVED");
    public static final Type PEER_FAILED = new Type("PEER_FAILED");
    public static final Type COORDINATOR_ELECTED = new Type("COORDINATOR_ELECTED");

    private static final class Type {
        private final String type;
        private Type(String type) {
            this.type = type;
        }
        
        public String toString() {
            return type;
        }
    }
    
    private final Cluster cluster;
    private final Peer peer;
    private final Type type;

    public ClusterEvent(Cluster cluster, Peer node, Type type) {
        this.cluster = cluster;
        this.peer = node;
        this.type = type;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Peer getPeer() {
        return peer;
    }

    public Type getType() {
        return type;
    }
}
