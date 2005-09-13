/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.io.impl;

import javax.jms.Destination;

import org.activecluster.Cluster;
import org.codehaus.wadi.io.PipeConfig;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

public class ClientClusterPipe extends AbstractClusterPipe {

    protected final SynchronizedInt _count=new SynchronizedInt(0);
    protected final String _theirCorrelationId;
    
    public ClientClusterPipe(PipeConfig config, long timeout, Cluster cluster, Destination us, Destination them, String correlationId, Channel inputQueue) {
        super(config, timeout, cluster, us, correlationId+"-client", them, inputQueue);
        _theirCorrelationId=correlationId+"-server";
    }
    
    protected String _theirCorrelationIdWithSuffix;
    
    public synchronized boolean run(Peer peer) throws Exception {
        int i=_count.increment();
        _theirCorrelationIdWithSuffix=_theirCorrelationId+"-"+i;
        return super.run(peer);
    }
    
    public String getTheirCorrelationId() {
        return _theirCorrelationIdWithSuffix;
    }
    
}
