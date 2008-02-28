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

import java.io.Serializable;

import org.codehaus.wadi.group.Peer;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CountingEnvelopeInfo implements Serializable {
    private final Peer hostingPeer;
    private long inboundEnvelopeCpt;
    private long outboundEnvelopeCpt;
    
    public CountingEnvelopeInfo(Peer hostingPeer, long inboundEnvelopeCpt, long outboundEnvelopeCpt) {
        if (null == hostingPeer) {
            throw new IllegalArgumentException("hostingPeer is required");
        }
        this.hostingPeer = hostingPeer;
        this.inboundEnvelopeCpt = inboundEnvelopeCpt;
        this.outboundEnvelopeCpt = outboundEnvelopeCpt;
    }
    
    public long getInboundEnvelopeCpt() {
        return inboundEnvelopeCpt;
    }

    public long getOutboundEnvelopeCpt() {
        return outboundEnvelopeCpt;
    }

    public Peer getHostingPeer() {
        return hostingPeer;
    }
    
}
