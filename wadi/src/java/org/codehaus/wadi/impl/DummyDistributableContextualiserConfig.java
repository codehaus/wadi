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
package org.codehaus.wadi.impl;

import java.net.InetSocketAddress;

import org.codehaus.wadi.DistributableContextualiserConfig;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.io.Server;

public class DummyDistributableContextualiserConfig extends DummyContextualiserConfig implements DistributableContextualiserConfig {

    protected final ExtendedCluster _cluster;
    
    public DummyDistributableContextualiserConfig(ExtendedCluster cluster) {
        super();
        _cluster=cluster;
    }

    public ExtendedCluster getCluster() {
        return _cluster;
    }

    public Server getServer() {
        return null;
    }
    
    public String getNodeId() {
        return "dummy";
    }
    
    public HttpProxy getHttpProxy() {
        return null;
    }

    public InetSocketAddress getHttpAddress() {
        return null;
    }
}
