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

import java.util.Map;

/**
 * 
 * @version $Revision: 1603 $
 */
public interface Cluster {

    void setElectionStrategy(ElectionStrategy strategy);

    Map getRemotePeers();
    
    int getPeerCount();

    LocalPeer getLocalPeer();
    
    Address getAddress();
    
    void addClusterListener(ClusterListener listener);

    void removeClusterListener(ClusterListener listener);

    void start() throws ClusterException;

    void stop() throws ClusterException;

    /**
     * @param membershipCount - when membership reaches this number or we timeout this method will return
     * @param timeout - the number of milliseconds to wait for membership to hit membershipCount
     * @return whether or not expected membershipCount was hit within given time
     * @throws InterruptedException
     */
    boolean waitOnMembershipCount(int membershipCount, long timeout) throws InterruptedException;
    
    /**
     * @return - the number of millis that a Peer may remain silent before being declared suspect/dead..
     */
    long getInactiveTime();
    
}
