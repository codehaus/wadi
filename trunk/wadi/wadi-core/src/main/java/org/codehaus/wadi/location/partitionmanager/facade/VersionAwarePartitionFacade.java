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
package org.codehaus.wadi.location.partitionmanager.facade;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.balancing.PartitionInfo;
import org.codehaus.wadi.location.partitionmanager.Partition;
import org.codehaus.wadi.location.partitionmanager.UnknownPartition;
import org.codehaus.wadi.location.partitionmanager.local.LocalPartition;
import org.codehaus.wadi.location.partitionmanager.remote.RemotePartition;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.SessionRequestMessage;
import org.codehaus.wadi.location.session.SessionResponseMessage;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * @version $Revision:1815 $
 */
public class VersionAwarePartitionFacade implements PartitionFacade {
    private static final Log log = LogFactory.getLog(VersionAwarePartitionFacade.class);
    
    private final int key;
    private final Latch partitionDefinedLatch;
    private final Dispatcher dispatcher;
    private final long partitionUpdateWaitTime;
    private final Object partitionInfoLock = new Object();
    private PartitionInfo partitionInfo;
    private Partition partition;
    private Latch partitionInfoLatch;

    public VersionAwarePartitionFacade(int key,
            Dispatcher dispatcher,
            PartitionInfo partitionInfo,
            long partitionUpdateWaitTime) {
        if (0 > key) {
            throw new IllegalArgumentException("key must be greater than 0");
        } else if (null == dispatcher ) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (null == partitionInfo) {
            throw new IllegalArgumentException("partitionInfo is required");
        } else if (0 > partitionUpdateWaitTime) {
            throw new IllegalArgumentException("partitionUpdateWaitTime must be >= 0");
        }
        this.key = key;
        this.dispatcher = dispatcher;
        this.partitionInfo = partitionInfo;
        this.partitionUpdateWaitTime = partitionUpdateWaitTime;

        partitionDefinedLatch = new Latch();
        
        partition = new UnknownPartition(key);
        partitionInfoLatch = new Latch();
    }
    
    public boolean waitForBoot(long attemptPeriod) throws InterruptedException {
        return partitionDefinedLatch.attempt(attemptPeriod);
    }

    public Envelope exchange(SessionRequestMessage request, long timeout) throws MessageExchangeException {
        PartitionInfo localPartitionInfo;
        Partition localPartition;
        Latch localRequestLatch;
        synchronized (partitionInfoLock) {
            localPartitionInfo = partitionInfo;
            localPartition = partition;
            localRequestLatch = partitionInfoLatch;
        }
        request.setVersion(localPartitionInfo.getVersion());
        request.setNumberOfExpectedMerge(localPartitionInfo.getNumberOfExpectedMerge());
        
        Envelope envelope;
        try {
            envelope = localPartition.exchange(request, timeout);
        } catch (MessageExchangeException e) {
            envelope = waitForUpdateAndExchange(request, timeout, localRequestLatch);
            if (null == envelope) {
                throw e;
            }
            return envelope;
        }
        Serializable payload = envelope.getPayload();
        if (!(payload instanceof SessionResponseMessage)) {
            return envelope;
        }
        
        SessionResponseMessage response = (SessionResponseMessage) payload;
        if (response.isVersionTooLow()) {
            Envelope newEnvelope = waitForUpdateAndExchange(request, timeout, localRequestLatch);
            if (null != newEnvelope) {
                return newEnvelope;
            }
            throw new PartitionFacadeVersionTooLowException("");
        } else if (response.isVersionTooHigh()) {
            throw new PartitionFacadeVersionTooHighException("");
        }
        
        return envelope;
    }

    public int getKey() {
        return key;
    }

    public boolean isLocal() {
        synchronized (partitionInfoLock) {
            return partition.isLocal();
        }
    }

    public void onMessage(final Envelope message, final DeleteIMToPM request) {
        Runnable attemptAction = new Runnable() {
            public void run() {
                onMessage(message, request);
            }
        };
        PartitionRunnable delegateAction = new PartitionRunnable() {
            public void run() {
                partition.onMessage(message, request);
            }
        };
        onMessage(message, request, attemptAction, delegateAction);
    }


    public void onMessage(final Envelope message, final EvacuateIMToPM request) {
        Runnable attemptAction = new Runnable() {
            public void run() {
                onMessage(message, request);
            }
        };
        PartitionRunnable delegateAction = new PartitionRunnable() {
            public void run() {
                partition.onMessage(message, request);
            }
        };
        onMessage(message, request, attemptAction, delegateAction);
    }

    public void onMessage(final Envelope message, final InsertIMToPM request) {
        Runnable attemptAction = new Runnable() {
            public void run() {
                onMessage(message, request);
            }
        };
        PartitionRunnable delegateAction = new PartitionRunnable() {
            public void run() {
                partition.onMessage(message, request);
            }
        };
        onMessage(message, request, attemptAction, delegateAction);
    }

    public void onMessage(final Envelope message, final MoveIMToPM request) {
        Runnable attemptAction = new Runnable() {
            public void run() {
                onMessage(message, request);
            }
        };
        PartitionRunnable delegateAction = new PartitionRunnable() {
            public void run() {
                partition.onMessage(message, request);
            }
        };
        onMessage(message, request, attemptAction, delegateAction);
    }

    public Partition setContent(PartitionInfo partitionInfo, LocalPartition content) {
        return setPartitionInfo(partitionInfo, content);
    }
    
    public Partition setContentRemote(PartitionInfo partitionInfo, Peer peer) {
        return setPartitionInfo(partitionInfo, new RemotePartition(key, dispatcher, peer));
    }
    
    public PartitionInfo getPartitionInfo() {
        synchronized (partitionInfoLock) {
            return partitionInfo;
        }
    }

    public void setPartitionInfo(PartitionInfo partitionInfo) {
        setPartitionInfo(partitionInfo, null);
    }

    protected Partition setPartitionInfo(PartitionInfo partitionInfo, Partition partition) {
        if (null == partitionInfo) {
            throw new IllegalArgumentException("partitionInfo is required");
        }
        
        Partition oldPartition;
        Latch oldPartitionInfoLatch;
        synchronized (partitionInfoLock) {
            oldPartition = this.partition;
            oldPartitionInfoLatch = partitionInfoLatch;
            partitionInfoLatch = new Latch();

            if (null == partition) {
                if (this.partitionInfo.getVersion() != partitionInfo.getVersion()) {
                    this.partitionInfo = partitionInfo;
                }
            } else if (partition instanceof LocalPartition) {
                LocalPartition newLocalPartition = (LocalPartition) partition;
                if (this.partitionInfo.getVersion() == partitionInfo.getVersion()) {
                    if (this.partition instanceof LocalPartition) {
                        ((LocalPartition) this.partition).merge(newLocalPartition);
                        this.partitionInfo.incrementNumberOfCurrentMerge();
                    }
                } else {
                    this.partitionInfo = partitionInfo;
                    if (partitionInfo.getNumberOfExpectedMerge() > 0) {
                        partitionInfo.incrementNumberOfCurrentMerge();
                    }
                    this.partition = partition;
                }
            } else {
                this.partitionInfo = partitionInfo;
                this.partition = partition;
            }
        }
        oldPartitionInfoLatch.release();

        partitionDefinedLatch.release();
        
        return oldPartition;
    }
    
    protected Envelope waitForUpdateAndExchange(SessionRequestMessage request, long timeout, Latch latch)  
        throws MessageExchangeException {
        boolean success;
        try {
            success = latch.attempt(timeout);
            if (success) {
                return exchange(request, timeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    protected void onMessage(Envelope message, SessionRequestMessage request, Runnable attemptAction, 
            PartitionRunnable delegateAction) {
        PartitionInfo localPartitionInfo;
        Partition localPartition;
        Latch localPartitionInfoLatch;
        synchronized (partitionInfoLock) {
            localPartitionInfo = partitionInfo;
            localPartition = partition;
            localPartitionInfoLatch = partitionInfoLatch;
        }
        
        if (localPartitionInfo.getVersion() > request.getVersion()) {
            handleVersionTooLow(message, request.newResponseFailure());
        } else if (localPartitionInfo.getVersion() < request.getVersion()) {
            handleVersionTooHigh(message, request, attemptAction, localPartitionInfoLatch);
        } else if (localPartitionInfo.getNumberOfCurrentMerge() < request.getNumberOfExpectedMerge()) {
            handleVersionTooHigh(message, request, attemptAction, localPartitionInfoLatch);
        } else {
            delegateAction.setPartition(localPartition);
            delegateAction.run();
        }
    }
    
    protected void handleVersionTooHigh(Envelope message, SessionRequestMessage request, Runnable attemptAction, 
            Latch latch) {
        boolean success = false;
        try {
            success = latch.attempt(partitionUpdateWaitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (success) {
            attemptAction.run();
        } else {
            SessionResponseMessage reply = request.newResponseFailure();
            reply.setVersionTooHigh(true);
            try {
                dispatcher.reply(message, reply);
            } catch (MessageExchangeException e) {
                log.error("See nested", e);
            }
        }
    }

    protected void handleVersionTooLow(Envelope message, SessionResponseMessage responseMessage) {
        responseMessage.setVersionTooLow(true);
        try {
            dispatcher.reply(message, responseMessage);
        } catch (MessageExchangeException e) {
            log.error("See nested", e);
        }
    }

    protected abstract class PartitionRunnable implements Runnable {
        protected Partition partition;
        
        public void setPartition(Partition partition) {
            this.partition = partition;
        }
    }
    
}
