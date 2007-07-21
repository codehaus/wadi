/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.location.partitionmanager;

import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.location.partitionmanager.facade.PartitionFacadeException;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.SessionRequestMessage;

/**
 * A Partition represents a 'bucket' in the Key:Location that is shared between
 * Cluster members.
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Partition {

    /**
     * @return whether or not this is a LocalPartition
     */
    boolean isLocal();

    /**
     * @return the Partition 'key' - a number between '0' and 'numPartitions-1'
     */
    int getKey();

    /**
     * A Peer has created a Session...
     * 
     * @param message
     * @param request
     */
    void onMessage(Envelope message, InsertIMToPM request);

    /**
     * A Peer has destroyed a Session...
     * 
     * @param message
     * @param request
     */
    void onMessage(Envelope message, DeleteIMToPM request);

    /**
     * A Peer wishes to evacuate a Session...
     * 
     * @param message
     * @param request
     */
    void onMessage(Envelope message, EvacuateIMToPM request);

    /**
     * A Peer has an Invocation for a Session of which it is not the owner...
     * 
     * @param message
     * @param request
     */
    void onMessage(Envelope message, MoveIMToPM request);

    /**
     * Send a message/request to the Partition and wait for a message/response...
     * 
     * @param request The request
     * @param timeout The number of milliseconds to wait for a response
     * @return the response
     * @throws MessageExchangeException
     */
    Envelope exchange(SessionRequestMessage request, long timeout) throws MessageExchangeException, PartitionFacadeException;
}
