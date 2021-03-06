/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.servicespace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.servicespace.replyaccessor.DoNotReplyWithNull;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class InvocationMetaDataTest extends RMockTestCase {

    public void testPrototype() throws Exception {
        InvocationMetaData prototype = new InvocationMetaData();
        prototype.setClusterAggregation(true);
        prototype.setIgnoreMessageExchangeExceptionOnSend(true);
        
        InvocationResultCombiner resultCombiner = (InvocationResultCombiner) mock(InvocationResultCombiner.class);
        prototype.setInvocationResultCombiner(resultCombiner);
        prototype.setOneWay(true);
        ReplyRequiredAssessor replyRequiredAssessor = (ReplyRequiredAssessor) mock(ReplyRequiredAssessor.class);
        prototype.setReplyAssessor(replyRequiredAssessor);
        
        Peer peer = (Peer) mock(Peer.class);
        final Peer[] peers = new Peer[] {peer};
        prototype.setTargets(peers);
        
        int timeout = 1000;
        prototype.setTimeout(timeout);
        
        InvocationMetaData copy = new InvocationMetaData(prototype);
        assertTrue(prototype.isClusterAggregation());
        assertTrue(prototype.isIgnoreMessageExchangeExceptionOnSend());
        assertSame(resultCombiner, copy.getInvocationResultCombiner());
        assertTrue(copy.isOneWay());
        assertSame(replyRequiredAssessor, copy.getReplyAssessor());
        assertSame(peers, copy.getTargets());
        assertEquals(timeout, copy.getTimeout());
    }

    public void testExternalizable() throws Exception {
        InvocationMetaData invocationMetaData = new InvocationMetaData();
        invocationMetaData.setOneWay(true);
        invocationMetaData.setClusterAggregation(true);
        invocationMetaData.setIgnoreMessageExchangeExceptionOnSend(true);
        invocationMetaData.setReplyAssessor(DoNotReplyWithNull.ASSESSOR);
        
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(memOut);
        out.writeObject(invocationMetaData);
        out.close();
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(memIn);
        InvocationMetaData newInvocationMetaData = (InvocationMetaData) in.readObject();
        assertTrue(newInvocationMetaData.isOneWay());
        assertFalse(newInvocationMetaData.isClusterAggregation());
        assertFalse(newInvocationMetaData.isIgnoreMessageExchangeExceptionOnSend());
        assertEquals(DoNotReplyWithNull.ASSESSOR, newInvocationMetaData.getReplyAssessor());
    }
    
}
