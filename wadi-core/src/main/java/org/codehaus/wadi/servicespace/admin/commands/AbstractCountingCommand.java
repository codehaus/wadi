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

import java.util.List;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.EnvelopeInterceptor;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.servicespace.InvocationResultCombiner;
import org.codehaus.wadi.servicespace.admin.Command;
import org.codehaus.wadi.servicespace.basic.ServiceSpaceRegistry;

/**
 * 
 * @version $Revision: 1538 $
 */
public abstract class AbstractCountingCommand implements Command {
    
    public Object execute(Dispatcher underlyingDispatcher, LocalPeer localPeer, ServiceSpaceRegistry registry) {
        Dispatcher disptacher = getDispatcher(underlyingDispatcher, localPeer, registry);
        if (null == disptacher) {
            return null;
        }
        return deriveCountingEnvelopeInfo(localPeer, disptacher);
    }

    protected abstract Dispatcher getDispatcher(Dispatcher underlyingDispatcher,
        LocalPeer localPeer,
        ServiceSpaceRegistry registry);

    protected Object deriveCountingEnvelopeInfo(LocalPeer localPeer, Dispatcher disptacher) {
        CountingEnvelopeInterceptor countingInterceptor = null;
        List<EnvelopeInterceptor> interceptors = disptacher.getInterceptors();
        for (EnvelopeInterceptor interceptor : interceptors) {
            if (interceptor instanceof CountingEnvelopeInterceptor) {
                countingInterceptor = (CountingEnvelopeInterceptor) interceptor;
                break;
            }
        }
        if (null == countingInterceptor) {
            disptacher.addInterceptor(new CountingEnvelopeInterceptor());
            return new CountingEnvelopeInfo(localPeer, 0, 0);
        }
        
        return new CountingEnvelopeInfo(localPeer, countingInterceptor.getInCpt(), countingInterceptor.getOutCpt());
    }
    
    public InvocationResultCombiner getInvocationResultCombiner() {
        return CountingEnvelopeInfoCombiner.COMBINER;
    }
    
}
