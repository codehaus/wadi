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
package org.codehaus.wadi.sandbox.io.impl;

import javax.jms.Destination;

import org.activecluster.Cluster;
import org.codehaus.wadi.sandbox.io.PipeConfig;

import EDU.oswego.cs.dl.util.concurrent.Channel;

public class ServerClusterPipe extends AbstractClusterPipe {

    protected final String _theirCorrelationId;
    
    public ServerClusterPipe(PipeConfig config, long timeout, Cluster cluster, Destination us, String ourId, Destination them, String theirId, Channel inputQueue) {
        super(config, timeout, cluster, us, ourId, them, inputQueue);
        _theirCorrelationId=theirId;
    }

    public String getTheirCorrelationId() {
        return _theirCorrelationId;
    }

}
