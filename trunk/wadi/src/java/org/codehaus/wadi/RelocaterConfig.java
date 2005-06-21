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
package org.codehaus.wadi;

import java.net.InetSocketAddress;
import java.util.Map;

import javax.jms.Destination;

import org.activecluster.Cluster;
import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.impl.MessageDispatcher;
import org.codehaus.wadi.io.Server;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

public interface RelocaterConfig extends Config {

    Collapser getCollapser();
    MessageDispatcher getDispatcher();
    Location getLocation();
    Map getMap();
    Cluster getCluster();
    Contextualiser getContextualiser();
    Server getServer();
    String getNodeName();
    SynchronizedBoolean getShuttingDown();
    HttpProxy getHttpProxy();
    InetSocketAddress getHttpAddress();
    
    DIndex getDIndex();
    void notifySessionRelocation(String name);
    
}
