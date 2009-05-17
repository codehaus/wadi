/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.servicespace.admin.commands;

import java.util.concurrent.atomic.AtomicLong;

import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.NoOpEnvelopeInterceptor;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CountingEnvelopeInterceptor extends NoOpEnvelopeInterceptor {
    private final AtomicLong inCpt;
    private final AtomicLong outCpt;
    
    public CountingEnvelopeInterceptor() {
        inCpt = new AtomicLong();
        outCpt = new AtomicLong();
    }

    public void reset() {
        inCpt.set(0);
        outCpt.set(0);
    }
    
    public long getInCpt() {
        return inCpt.get();
    }
    
    public long getOutCpt() {
        return outCpt.get();
    }
    
    public Envelope onInboundEnvelope(Envelope envelope) throws MessageExchangeException {
        inCpt.incrementAndGet();
        return envelope;
    }

    public Envelope onOutboundEnvelope(Envelope envelope) throws MessageExchangeException {
        outCpt.incrementAndGet();
        return envelope;
    }

}
