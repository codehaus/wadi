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
package org.codehaus.wadi.servicespace.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.servicespace.InvocationInfo;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceInvoker implements ServiceInvoker {
    private static final Log log = LogFactory.getLog(BasicServiceInvoker.class);
    
    private final ServiceSpace serviceSpace;
    private final Dispatcher dispatcher;
    private final ServiceName targetServiceName;

    public BasicServiceInvoker(ServiceSpace serviceSpace, ServiceName targetServiceName) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == targetServiceName) {
            throw new IllegalArgumentException("targetServiceName is required");
        }
        this.serviceSpace = serviceSpace;
        this.targetServiceName = targetServiceName;
        
        dispatcher = serviceSpace.getDispatcher();
    }

    public InvocationResult invoke(InvocationInfo invInfo) {
        Envelope message = dispatcher.createMessage();
        message.setPayload(invInfo);
        EnvelopeServiceHelper.setServiceName(targetServiceName, message);
        message.setReplyTo(dispatcher.getCluster().getLocalPeer().getAddress());

        if (invInfo.getMetaData().isClusterTargeted()) {
            return invokeClusterInvocation(invInfo, message);
        } else {
            return invokePeersInvocation(invInfo, message);
        }
    }

    protected InvocationResult invokePeersInvocation(InvocationInfo invInfo, Envelope envelope) {
        InvocationMetaData metaData = invInfo.getMetaData();
        Peer[] targetPeers = metaData.getTargets();
        Quipu quipu = null;
        if (!metaData.isOneWay()) {
            quipu = dispatcher.newRendezVous(targetPeers.length);
            envelope.setSourceCorrelationId(quipu.getCorrelationId());
        }

        try {
            sendInvocation(envelope, targetPeers);
        } catch (MessageExchangeException e) {
            return new InvocationResult(e);
        }
        
        if (metaData.isOneWay()) {
            return null;
        }
        
        Collection messages;
        try {
            messages = dispatcher.attemptMultiRendezVous(quipu, metaData.getTimeout());
        } catch (MessageExchangeException e) {
            return new InvocationResult(e);
        }
        
        return combineResults(metaData, messages);
    }

    protected void sendInvocation(Envelope envelope, Peer[] targetPeers) throws MessageExchangeException {
        for (int i = 0; i < targetPeers.length; i++) {
            Address address = targetPeers[i].getAddress();
            envelope.setAddress(address);
            dispatcher.send(address, envelope);
        }
    }

    protected InvocationResult combineResults(InvocationMetaData metaData, Collection messages) {
        InvocationResultCombiner combiner = metaData.getInvocationResultCombiner();
        Collection invocationResults = new ArrayList();
        for (Iterator iter = messages.iterator(); iter.hasNext();) {
            Envelope replyEnvelope = (Envelope) iter.next();
            InvocationResult result = (InvocationResult) replyEnvelope.getPayload();
            invocationResults.add(result);
        }
        return combiner.combine(invocationResults);
    }

    protected InvocationResult invokeClusterInvocation(InvocationInfo invInfo, Envelope message) {
        InvocationMetaData metaData = invInfo.getMetaData();
        Address target = dispatcher.getCluster().getAddress();
        if (metaData.isOneWay()) {
            try {
                dispatcher.send(target, message);
            } catch (MessageExchangeException e) {
                return new InvocationResult(e);
            }
            return null;
        } else {
            Envelope replyMessage;
            try {
                replyMessage = dispatcher.exchangeSend(target, message, metaData.getTimeout());
            } catch (MessageExchangeException e) {
                return new InvocationResult(e);
            }
            return (InvocationResult) replyMessage.getPayload();
        }
    }

}
