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

import java.io.IOException;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.EnvelopeListener;
import org.codehaus.wadi.servicespace.InvocationInfo;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpace;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: $
 */
public class ServiceInvocationListenerTest extends RMockTestCase {

    private ServiceSpace serviceSpace;
    private ServiceRegistry serviceRegistry;
    private EnvelopeListener next;
    private ServiceName serviceName;
    private Dispatcher dispatcher;

    protected void setUp() throws Exception {
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        dispatcher = serviceSpace.getDispatcher();
        serviceRegistry = serviceSpace.getServiceRegistry();
        next = (EnvelopeListener) mock(EnvelopeListener.class);
        serviceName = new ServiceName("name");
    }
    
    public void testNotServiceMessage() {
        Envelope envelope = (Envelope) mock(Envelope.class);
        
        beginSection(s.ordered("Check if service message and dispatch to next"));
        EnvelopeServiceHelper.getServiceName(envelope);
        next.onEnvelope(envelope);
        endSection();
        
        startVerification();
        
        ServiceInvocationListener invocationListener = new ServiceInvocationListener(serviceSpace, next);
        invocationListener.onEnvelope(envelope);
    }

    public void testOneWayServiceMessage() throws Exception {
        ServiceInterface service = (ServiceInterface) mock(ServiceInterface.class);
        Envelope envelope = (Envelope) mock(Envelope.class);
        
        beginSection(s.ordered("Process one-way message"));
        recordOneWayProcessing(service, envelope, true);
        endSection();
        
        startVerification();
        
        ServiceInvocationListener invocationListener = new ServiceInvocationListener(serviceSpace, next);
        invocationListener.onEnvelope(envelope);
    }

    public void testRequestReplyServiceMessage() throws Exception {
        ServiceInterface service = (ServiceInterface) mock(ServiceInterface.class);
        Envelope envelope = (Envelope) mock(Envelope.class);
        
        beginSection(s.ordered("Process request-reply message"));
        recordOneWayProcessing(service, envelope, false);
        recordSuccessfulReply(envelope);
        endSection();
        
        startVerification();
        
        ServiceInvocationListener invocationListener = new ServiceInvocationListener(serviceSpace, next);
        invocationListener.onEnvelope(envelope);
    }

    public void testServiceMethodThrowException() throws Exception {
        ServiceInterface service = (ServiceInterface) mock(ServiceInterface.class);
        Envelope envelope = (Envelope) mock(Envelope.class);
        
        beginSection(s.ordered("Process one-way message"));
        recordOneWayProcessing(service, envelope, false);
        recordExceptionReply(envelope);
        endSection();
        
        startVerification();
        
        ServiceInvocationListener invocationListener = new ServiceInvocationListener(serviceSpace, next);
        invocationListener.onEnvelope(envelope);
    }
    
    private void recordSuccessfulReply(Envelope envelope) throws MessageExchangeException {
        final String sayHelloResult = "sayHelloResult";
        modify().returnValue(sayHelloResult);
        
        Envelope reply = dispatcher.createEnvelope();
        EnvelopeServiceHelper.tagAsServiceReply(reply);
        reply.setPayload(null);
        modify().args(new AbstractExpression() {

            public void describeWith(ExpressionDescriber expressionDescriber) throws IOException {
            }

            public boolean passes(Object object) {
                InvocationResult result = (InvocationResult) object;
                assertTrue(result.isSuccess());
                assertSame(sayHelloResult, result.getResult());
                return true;
            }
            
        });
        
        dispatcher.reply(envelope, reply);
    }

    private void recordExceptionReply(Envelope envelope) throws MessageExchangeException {
        final Exception exception = new Exception();
        modify().throwException(exception);
        
        Envelope reply = dispatcher.createEnvelope();
        EnvelopeServiceHelper.tagAsServiceReply(reply);
        reply.setPayload(null);
        modify().args(new AbstractExpression() {

            public void describeWith(ExpressionDescriber expressionDescriber) throws IOException {
            }

            public boolean passes(Object object) {
                InvocationResult result = (InvocationResult) object;
                assertFalse(result.isSuccess());
                assertSame(exception, result.getThrowable());
                return true;
            }
            
        });
        
        dispatcher.reply(envelope, reply);
    }

    private void recordOneWayProcessing(ServiceInterface service, Envelope envelope, boolean oneWay) throws Exception {
        EnvelopeServiceHelper.getServiceName(envelope);
        modify().returnValue(serviceName);

        envelope.getPayload();
        String helloString = "hello";
        InvocationMetaData invocationMetaData = new InvocationMetaData();
        invocationMetaData.setOneWay(oneWay);
        modify().returnValue(new InvocationInfo("sayHello", new Class[] {String.class}, new Object[] {helloString},
                invocationMetaData));

        serviceRegistry.getStartedService(serviceName);
        modify().returnValue(service);

        service.sayHello(helloString);
    }

    public interface ServiceInterface {
        String sayHello(String value) throws Exception;
    }
    
}
