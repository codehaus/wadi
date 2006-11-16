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
package org.codehaus.wadi.location.impl;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;
import org.codehaus.wadi.location.Partition;
import org.codehaus.wadi.location.SessionRequestMessage;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.partition.PartitionInfo;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class PartitionFacadeDelegate extends AbstractPartition implements PartitionFacade {
    private final Dispatcher dispatcher;
    private final SynchronizedRef synchronizedPartitionRef;
    private final Latch partitionDefinedLatch;

    public PartitionFacadeDelegate(int key, Dispatcher dispatcher) {
        super(key);
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        }
        this.dispatcher = dispatcher;
        
        synchronizedPartitionRef = new SynchronizedRef(new UnknownPartition(key));
        partitionDefinedLatch = new Latch();
    }

    public boolean waitForBoot(long attemptPeriod) throws InterruptedException {
        return partitionDefinedLatch.attempt(attemptPeriod);
    }
    
    public boolean waitForLocalization(PartitionInfo newPartitionInfo, long attemptPeriod) {
        return true;
    }
    
    public boolean isLocal() {
        return getContent().isLocal();
    }
    
    public void onMessage(Envelope message, InsertIMToPM request) {
        Partition localPartition = getContent();
        localPartition.onMessage(message, request);
    }

    public void onMessage(Envelope message, DeleteIMToPM request) {
        Partition localPartition = getContent();
        localPartition.onMessage(message, request);
    }

    public void onMessage(Envelope message, EvacuateIMToPM request) {
        Partition localPartition = getContent();
        localPartition.onMessage(message, request);
    }

    public void onMessage(Envelope message, MoveIMToPM request) {
        Partition localPartition = getContent();
        localPartition.onMessage(message, request);
    }

    public Envelope exchange(SessionRequestMessage request, long timeout) throws MessageExchangeException {
        Partition localPartition = getContent();
        return localPartition.exchange(request, timeout);
    }
    
    public void setPartitionInfo(PartitionInfo partitionInfo) {
        partitionDefinedLatch.release();
    }

    public Partition setContent(PartitionInfo partitionInfo, LocalPartition content) {
        Partition oldPartition = (Partition) synchronizedPartitionRef.set(content);
        partitionDefinedLatch.release();
        return oldPartition;
    }
    
    public Partition setContentRemote(PartitionInfo partitionInfo, Peer peer) {
        Partition oldPartition = (Partition) synchronizedPartitionRef.set(new RemotePartition(_key, dispatcher, peer));
        partitionDefinedLatch.release();
        return oldPartition;
    }

    public String toString() {
        return "PartitionFacade for [" + getContent() + "]";
    }
    
    protected Partition getContent() {
        return (Partition) synchronizedPartitionRef.get();
    }

	public Address getAddress() {
		return getContent().getAddress();
	}

	public String getName() {
		return getContent().getName();
	}

	public PeerInfo getPeerInfo() {
		return getContent().getPeerInfo();
	}
    
}
