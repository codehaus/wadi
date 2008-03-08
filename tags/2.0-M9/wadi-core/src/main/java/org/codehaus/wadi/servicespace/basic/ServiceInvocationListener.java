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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.reflect.ClassIndexer;
import org.codehaus.wadi.core.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.core.reflect.MemberUpdater;
import org.codehaus.wadi.core.reflect.MemberUpdaterException;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.EnvelopeListener;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.servicespace.InvocationInfo;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.ServiceException;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision: $
 */
public class ServiceInvocationListener implements EnvelopeListener {
    private static final Log log = LogFactory.getLog(ServiceInvocationListener.class);
    
    private final ServiceSpace serviceSpace;
    private final Dispatcher dispatcher;
    private final ServiceRegistry serviceRegistry;
    private final EnvelopeListener next;
    private final ClassIndexerRegistry classIndexerRegistry;
    
    public ServiceInvocationListener(ServiceSpace serviceSpace,
            ClassIndexerRegistry classIndexerRegistry,
            EnvelopeListener next) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == classIndexerRegistry) {
            throw new IllegalArgumentException("classIndexerRegistry is required");
        } else if (null == next) {
            throw new IllegalArgumentException("next is required");
        }
        this.serviceSpace = serviceSpace;
        this.classIndexerRegistry = classIndexerRegistry;
        this.next = next;
        
        dispatcher = serviceSpace.getDispatcher();
        serviceRegistry = serviceSpace.getServiceRegistry();
    }

    public void onEnvelope(Envelope message) {
        ServiceName serviceName = EnvelopeServiceHelper.getServiceName(message);
        if (null != serviceName) {
            handleServiceMessage(serviceName, message);
        } else {
            next.onEnvelope(message);
        }
    }

    private void handleServiceMessage(ServiceName serviceName, Envelope request) {
        InvocationInfo invMetaData = (InvocationInfo) request.getPayload();
        InvocationMetaData metaData = invMetaData.getMetaData();

        InvocationResult result;
        try {
            Object service = serviceRegistry.getStartedService(serviceName);
            result = invokeServiceMethod(service, invMetaData);
        } catch (ServiceException e) {
            result = new InvocationResult(e);
        }
        if (!metaData.isOneWay() && metaData.getReplyAssessor().isReplyRequired(result)) {
            Envelope reply = dispatcher.createEnvelope();
            EnvelopeServiceHelper.tagAsServiceReply(reply);
            reply.setPayload(result);
            try {
                dispatcher.reply(request, reply);
            } catch (MessageExchangeException e) {
                log.warn(e);
            }
        }
    }

    protected InvocationResult invokeServiceMethod(Object service, InvocationInfo invMetaData) {
        ClassIndexer classIndexer = classIndexerRegistry.getClassIndexer(invMetaData.getTargetClass());
        MemberUpdater memberUpdater = classIndexer.getMemberUpdater(invMetaData.getMemberUpdaterIndex());
        
        InvocationResult result;
        try {
            Object object = memberUpdater.executeWithParameters(service, invMetaData.getParams());
            result = new InvocationResult(object);
        } catch (MemberUpdaterException e) {
            result = new InvocationResult(e);
        }
        return result;
    }
    
}
