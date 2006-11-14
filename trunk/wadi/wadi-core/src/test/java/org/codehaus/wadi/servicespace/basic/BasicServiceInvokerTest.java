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
import java.util.Collection;
import java.util.Collections;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.servicespace.InvocationInfo;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.InvocationResult;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceInvokerTest extends RMockTestCase {

    private ServiceSpace serviceSpace;
    private ServiceName serviceName;
    private Dispatcher dispatcher;
    private Cluster cluster;
    private LocalPeer localPeer;
    private Peer peer1;
    private Peer peer2;
    private Address localAddress;
    private Address address1;
    private Address address2;

    protected void setUp() throws Exception {
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        dispatcher = serviceSpace.getDispatcher();
        serviceName = new ServiceName("name");
        
        cluster = dispatcher.getCluster();
        localPeer = cluster.getLocalPeer();
        localAddress = localPeer.getAddress();
        
        peer1 = (Peer) mock(Peer.class);
        address1 = peer1.getAddress();
        modify().multiplicity(expect.from(0));

        peer2 = (Peer) mock(Peer.class);
        address2 = peer2.getAddress();
        modify().multiplicity(expect.from(0));
    }
    
    public void testOneWayInvoke() throws Exception {
        InvocationMetaData invocationMetaData = new InvocationMetaData();
        invocationMetaData.setTargets(new Peer[] {peer1, peer2});
        invocationMetaData.setOneWay(true);
        InvocationInfo invocInfo = new InvocationInfo("name", new Class[0], new Object[0], invocationMetaData);
        
        beginSection(s.ordered("Prepare and send invocations"));
        Envelope envelope = recordPrepareOneWayMessage(invocInfo);
        
        recordSendInvocations(envelope);
        
        endSection();
        startVerification();
        
        BasicServiceInvoker serviceInvoker = new BasicServiceInvoker(serviceSpace, serviceName);
        serviceInvoker.invoke(invocInfo);
    }

    public void testRequestReplyInvoke() throws Exception {
        InvocationResultCombiner resultCombiner = (InvocationResultCombiner) mock(InvocationResultCombiner.class);
        
        InvocationMetaData invocationMetaData = new InvocationMetaData();
        invocationMetaData.setTargets(new Peer[] {peer1, peer2});
        invocationMetaData.setInvocationResultCombiner(resultCombiner);
        InvocationInfo invocInfo = new InvocationInfo("name", new Class[0], new Object[0], invocationMetaData);
        
        beginSection(s.ordered("Prepare and send invocations"));
        Envelope envelope = recordPrepareOneWayMessage(invocInfo);

        Quipu quipu = recordReplyMessage(invocationMetaData, envelope);
        
        recordSendInvocations(envelope);
        
        recordProcessResults(resultCombiner, invocationMetaData, quipu);
        
        endSection();
        startVerification();
        
        BasicServiceInvoker serviceInvoker = new BasicServiceInvoker(serviceSpace, serviceName);
        serviceInvoker.invoke(invocInfo);
    }

    public void testExceptionWhenSendingInvocation() throws Exception {
        InvocationMetaData invocationMetaData = new InvocationMetaData();
        invocationMetaData.setTargets(new Peer[] {peer1});
        invocationMetaData.setOneWay(true);
        InvocationInfo invocInfo = new InvocationInfo("name", new Class[0], new Object[0], invocationMetaData);
        
        beginSection(s.ordered("Prepare and send invocation"));
        Envelope envelope = recordPrepareOneWayMessage(invocInfo);
        
        envelope.setAddress(address1);
        dispatcher.send(address1, envelope);
        MessageExchangeException expectedException = new MessageExchangeException("");
        modify().throwException(expectedException);
        endSection();
        
        startVerification();
        
        BasicServiceInvoker serviceInvoker = new BasicServiceInvoker(serviceSpace, serviceName);
        InvocationResult result = serviceInvoker.invoke(invocInfo);
        assertFalse(result.isSuccess());
        assertSame(expectedException, result.getThrowable());
    }
    
    private void recordProcessResults(InvocationResultCombiner resultCombiner, InvocationMetaData invocationMetaData, Quipu quipu) throws MessageExchangeException {
        Envelope replyEnvelope = (Envelope) mock(Envelope.class);
        dispatcher.attemptMultiRendezVous(quipu, invocationMetaData.getTimeout());
        modify().returnValue(Collections.singleton(replyEnvelope));

        replyEnvelope.getPayload();
        final InvocationResult invocationResult = new InvocationResult(new Object());
        modify().returnValue(invocationResult);
        
        resultCombiner.combine(null);
        modify().args(new AbstractExpression() {

            public void describeWith(ExpressionDescriber expressionDescriber) throws IOException {
            }

            public boolean passes(Object object) {
                Collection collection = (Collection) object;
                assertEquals(1, collection.size());
                InvocationResult actualResult = (InvocationResult) collection.iterator().next();
                assertSame(invocationResult, actualResult);
                return true;
            }
            
        });
    }

    private Quipu recordReplyMessage(InvocationMetaData invocationMetaData, Envelope envelope) {
        int nbTargets = invocationMetaData.getTargets().length;
        dispatcher.newRendezVous(nbTargets);
        Quipu quipu = new Quipu(nbTargets, "1");
        modify().returnValue(quipu);
        
        envelope.setSourceCorrelationId(quipu.getCorrelationId());
        return quipu;
    }

    private Envelope recordPrepareOneWayMessage(InvocationInfo invocInfo) {
        Envelope envelope = dispatcher.createMessage();
        envelope.setPayload(invocInfo);
        EnvelopeServiceHelper.setServiceName(serviceName, envelope);
        envelope.setReplyTo(localAddress);
        return envelope;
    }

    private void recordSendInvocations(Envelope envelope) throws MessageExchangeException {
        beginSection(s.unordered("Send invocations"));
        beginSection(s.ordered("Send to address"));
        envelope.setAddress(address1);
        dispatcher.send(address1, envelope);
        endSection();
        
        beginSection(s.ordered("Send to address"));
        envelope.setAddress(address2);
        dispatcher.send(address2, envelope);
        endSection();
        endSection();
    }

}
