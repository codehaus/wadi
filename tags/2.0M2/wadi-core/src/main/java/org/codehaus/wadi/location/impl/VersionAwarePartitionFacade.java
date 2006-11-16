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
package org.codehaus.wadi.location.impl;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;
import org.codehaus.wadi.location.Partition;
import org.codehaus.wadi.location.SessionRequestMessage;
import org.codehaus.wadi.location.SessionResponseMessage;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.partition.PartitionInfo;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * @version $Revision:1815 $
 */
public class VersionAwarePartitionFacade implements PartitionFacade {
    private static final Log log = LogFactory.getLog(VersionAwarePartitionFacade.class);
    
    private final Dispatcher dispatcher;
    private final PartitionFacade delegate;
    private final long partitionUpdateWaitTime;
    private final Object partitionInfoLock = new Object();
    private PartitionInfo partitionInfo;
    private Latch partitionInfoLatch;

    public VersionAwarePartitionFacade(Dispatcher dispatcher, PartitionFacade delegate, long partitionUpdateWaitTime) {
        if (null == dispatcher ) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (null == delegate) {
            throw new IllegalArgumentException("delegate is required");
        } else if (0 > partitionUpdateWaitTime) {
            throw new IllegalArgumentException("partitionUpdateWaitTime must be >= 0");
        }
        this.dispatcher = dispatcher;
        this.delegate = delegate;
        this.partitionUpdateWaitTime = partitionUpdateWaitTime;
        
        partitionInfoLatch = new Latch();
    }
    
    public boolean waitForBoot(long attemptPeriod) throws InterruptedException {
        return delegate.waitForBoot(attemptPeriod);
    }

    public Envelope exchange(SessionRequestMessage request, long timeout) throws MessageExchangeException {
        PartitionInfo localPartitionInfo;
        Latch localRequestLatch;
        synchronized (partitionInfoLock) {
            localPartitionInfo = partitionInfo;
            localRequestLatch = partitionInfoLatch;
        }
        request.setVersion(localPartitionInfo.getVersion());
        
        Envelope envelope;
        try {
            envelope = delegate.exchange(request, timeout);
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
        return delegate.getKey();
    }

    public boolean isLocal() {
        return delegate.isLocal();
    }

    public void onMessage(final Envelope message, final DeleteIMToPM request) {
        Runnable attemptAction = new Runnable() {
            public void run() {
                onMessage(message, request);
            }
        };
        Runnable delegateAction = new Runnable() {
            public void run() {
                delegate.onMessage(message, request);
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
        Runnable delegateAction = new Runnable() {
            public void run() {
                delegate.onMessage(message, request);
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
        Runnable delegateAction = new Runnable() {
            public void run() {
                delegate.onMessage(message, request);
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
        Runnable delegateAction = new Runnable() {
            public void run() {
                delegate.onMessage(message, request);
            }
        };
        onMessage(message, request, attemptAction, delegateAction);
    }

    public boolean waitForLocalization(PartitionInfo newPartitionInfo, long attemptPeriod) throws InterruptedException {
        PartitionInfo localPartitionInfo;
        Latch localPartitionInfoLatch;
        synchronized (partitionInfoLock) {
            localPartitionInfo = partitionInfo;
            localPartitionInfoLatch = partitionInfoLatch;
        }
        if (null == localPartitionInfo || localPartitionInfo.getVersion() < newPartitionInfo.getVersion()) {
            boolean success = localPartitionInfoLatch.attempt(attemptPeriod);
            if (!success) {
                return false;
            }
            return waitForLocalization(newPartitionInfo, attemptPeriod);
        }
        return true;
    }
    
    public Partition setContent(PartitionInfo partitionInfo, LocalPartition content) {
        Partition oldPartition = delegate.setContent(partitionInfo, content);
        setPartitionInfo(partitionInfo);
        return oldPartition;
    }
    
    public Partition setContentRemote(PartitionInfo partitionInfo, Peer peer) {
        Partition oldPartition = delegate.setContentRemote(partitionInfo, peer);
        setPartitionInfo(partitionInfo);
        return oldPartition;
    }
    
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public PartitionInfo getPartitionInfo() {
        return partitionInfo;
    }

    public void setPartitionInfo(PartitionInfo partitionInfo) {
        if (null == partitionInfo) {
            throw new IllegalArgumentException("partitionInfo is required");
        }
        
        Latch oldPartitionInfoLatch;
        synchronized (partitionInfoLock) {
            this.partitionInfo = partitionInfo;
            oldPartitionInfoLatch = partitionInfoLatch;
            partitionInfoLatch = new Latch();
        }
        oldPartitionInfoLatch.release();
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
            Runnable delegateAction) {
        PartitionInfo localPartitionInfo;
        Latch localPartitionInfoLatch;
        synchronized (partitionInfoLock) {
            localPartitionInfo = partitionInfo;
            localPartitionInfoLatch = partitionInfoLatch;
        }
        
        if (localPartitionInfo.getVersion() > request.getVersion()) {
            handleVersionTooLow(message, request.newResponseFailure());
        } else if (localPartitionInfo.getVersion() < request.getVersion()) {
            handleVersionTooHigh(message, request, attemptAction, localPartitionInfoLatch);
        } else {
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

	public Address getAddress() {
		return delegate.getAddress();
	}

	public String getName() {
		return delegate.getName();
	}

	public PeerInfo getPeerInfo() {
		return delegate.getPeerInfo();
	}

}
