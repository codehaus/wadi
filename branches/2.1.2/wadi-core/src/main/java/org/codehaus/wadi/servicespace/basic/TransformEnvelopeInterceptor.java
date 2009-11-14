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

package org.codehaus.wadi.servicespace.basic;

import java.io.IOException;

import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.NoOpEnvelopeInterceptor;

/**
 * 
 * @version $Revision: 1538 $
 */
public class TransformEnvelopeInterceptor extends NoOpEnvelopeInterceptor {
    private final ServiceSpaceEnvelopeHelper envelopeHelper;
    
    public TransformEnvelopeInterceptor(ServiceSpaceEnvelopeHelper envelopeHelper) {
        if (null == envelopeHelper) {
            throw new IllegalArgumentException("envelopeHelper is required");
        }
        this.envelopeHelper = envelopeHelper;
    }

    public Envelope onInboundEnvelope(Envelope envelope) throws MessageExchangeException {
        try {
            envelopeHelper.transformInboundEnvelope(envelope);
        } catch (Exception e) {
            throw new MessageExchangeException(e);
        }
        return envelope;
    }

    public Envelope onOutboundEnvelope(Envelope envelope) throws MessageExchangeException {
        try {
            envelopeHelper.transformOutboundEnvelope(envelope);
        } catch (IOException e) {
            throw new MessageExchangeException(e);
        }
        return envelope;
    }

}