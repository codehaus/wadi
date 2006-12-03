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

import java.io.IOException;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.location.SessionRequestMessage;
import org.codehaus.wadi.location.SessionResponseMessage;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.DeletePMToIM;
import org.codehaus.wadi.partition.PartitionInfo;

import com.agical.rmock.core.Action;
import com.agical.rmock.core.MethodHandle;
import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * @version $Revision:1815 $
 */
public class VersionAwarePartitionFacadeTest extends RMockTestCase {
    private static final long TIMEOUT = 4000;
    private PartitionInfo PARTITION_INFO_V0 = new PartitionInfo(0, 0);
    private PartitionInfo PARTITION_INFO_V1 = new PartitionInfo(1, 0);

    private Dispatcher dispatcher;
    private PartitionFacade delegate;
    private VersionAwarePartitionFacade facade;

    protected void setUp() throws Exception {
        dispatcher = (Dispatcher) mock(Dispatcher.class);
        delegate = (PartitionFacade) mock(PartitionFacade.class);
        facade = new VersionAwarePartitionFacade(dispatcher, new PartitionInfo(0, 0), delegate, TIMEOUT);
        facade.setPartitionInfo(PARTITION_INFO_V0);
    }

    public void testBasicExchange() throws Exception {
        SessionRequestMessage reqMsg = recordSuccessFulExchange(null, PARTITION_INFO_V0);
        startVerification();
        
        facade.exchange(reqMsg, TIMEOUT);
    }

    public void testCannotExchange() throws Exception {
        beginSection(s.ordered("Exchange with communication failure"));
        SessionRequestMessage reqMsg = (SessionRequestMessage) mock(SessionRequestMessage.class);
        reqMsg.setVersion(PARTITION_INFO_V0.getVersion());

        delegate.exchange(reqMsg, TIMEOUT);
        modify().throwException(new MessageExchangeException(""));
        endSection();
        startVerification();

        long start = System.currentTimeMillis();
        try {
            facade.exchange(reqMsg, TIMEOUT);
            fail();
        } catch (MessageExchangeException e) {
        }
        assertTrue((System.currentTimeMillis() - start) > TIMEOUT - 100);
    }

    public void testFirstExchangeFailsAndSecondCompletes() throws Exception {
        beginSection(s.ordered("Exchange with communication failure"));
        SessionRequestMessage reqMsg = (SessionRequestMessage) mock(SessionRequestMessage.class);
        reqMsg.setVersion(PARTITION_INFO_V0.getVersion());

        delegate.exchange(reqMsg, TIMEOUT);
        modify().perform(new Action() {

            public Object invocation(Object[] arguments, MethodHandle methodHandle) throws Throwable {
                facade.setPartitionInfo(PARTITION_INFO_V1);
                throw new MessageExchangeException("");
            }
            
        });
        recordSuccessFulExchange(reqMsg, PARTITION_INFO_V1);
        endSection();
        startVerification();

        facade.exchange(reqMsg, TIMEOUT);
    }

    public void testDeleteIMToPMWithRightVersion() throws Exception {
        DeleteIMToPM deleteIMToPM = new DeleteIMToPM("name");
        deleteIMToPM.setVersion(PARTITION_INFO_V0.getVersion());
        
        Envelope env = (Envelope) mock(Envelope.class);

        beginSection(s.ordered("straight delegate to delegate"));
        delegate.onMessage(env, deleteIMToPM);
        endSection();
        startVerification();

        facade.onMessage(env, deleteIMToPM);
    }

    public void testDeleteIMToPMWithTooLowVersion() throws Exception {
        DeleteIMToPM deleteIMToPM = new DeleteIMToPM("name");
        deleteIMToPM.setVersion(PARTITION_INFO_V0.getVersion());
        
        Envelope env = (Envelope) mock(Envelope.class);

        beginSection(s.ordered("Reply with failure and versionTooLow attribute set"));
        dispatcher.reply(env, new DeletePMToIM(false));
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber expressionDescriber) throws IOException {
            }

            public boolean passes(Object object) {
                DeletePMToIM deletePMToIM = (DeletePMToIM) object;
                return !deletePMToIM.isSuccess() && deletePMToIM.isVersionTooLow() && !deletePMToIM.isVersionTooHigh();
            }
            
        });
        endSection();
        startVerification();

        facade.setPartitionInfo(PARTITION_INFO_V1);
        facade.onMessage(env, deleteIMToPM);
    }

    public void testDeleteIMToPMWithTooHighVersionTimeOut() throws Exception {
        DeleteIMToPM deleteIMToPM = new DeleteIMToPM("name");
        deleteIMToPM.setVersion(PARTITION_INFO_V1.getVersion());
        
        Envelope env = (Envelope) mock(Envelope.class);
        dispatcher.reply(env, new DeletePMToIM(false));
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber expressionDescriber) throws IOException {
            }

            public boolean passes(Object object) {
                DeletePMToIM deletePMToIM = (DeletePMToIM) object;
                return !deletePMToIM.isSuccess() && !deletePMToIM.isVersionTooLow() && deletePMToIM.isVersionTooHigh();
            }
            
        });
        startVerification();

        facade.onMessage(env, deleteIMToPM);
    }

    public void testDeleteIMToPMWithTooHighVersionAndReAttempt() throws Exception {
        DeleteIMToPM deleteIMToPM = new DeleteIMToPM("name");
        deleteIMToPM.setVersion(PARTITION_INFO_V1.getVersion());
        
        Envelope env = (Envelope) mock(Envelope.class);

        delegate.onMessage(env, deleteIMToPM);
        startVerification();

        Thread setV1Partition = new Thread() {
            public void run() {
                try {
                    Thread.sleep(TIMEOUT / 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                facade.setPartitionInfo(PARTITION_INFO_V1);
            }
        };
        setV1Partition.start();
        facade.onMessage(env, deleteIMToPM);
    }

    private SessionRequestMessage recordSuccessFulExchange(SessionRequestMessage reqMsg, PartitionInfo partitionInfo) throws MessageExchangeException {
        beginSection(s.ordered("Exchange with basic result"));
        if (null == reqMsg) {
            reqMsg = (SessionRequestMessage) mock(SessionRequestMessage.class);
        }
        reqMsg.setVersion(partitionInfo.getVersion());

        Envelope respEnv = (Envelope) mock(Envelope.class);
        delegate.exchange(reqMsg, TIMEOUT);
        modify().returnValue(respEnv);
        
        SessionResponseMessage respMsg = (SessionResponseMessage) mock(SessionResponseMessage.class);
        respEnv.getPayload();
        modify().returnValue(respMsg);

        respMsg.isVersionTooLow();
        modify().returnValue(false);
        
        respMsg.isVersionTooHigh();
        modify().returnValue(false);
        endSection();
        return reqMsg;
    }

}
