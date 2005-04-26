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
package org.codehaus.wadi.sandbox.impl;

import javax.jms.JMSException;
import javax.jms.Topic;

import org.activecluster.Cluster;
import org.activecluster.ClusterException;
import org.activecluster.ClusterFactory;

public class RestartableClusterFactory implements ClusterFactory {

    protected final ClusterFactory _factory;
    
    public RestartableClusterFactory(ClusterFactory factory) {
        super();
        _factory=factory;
    }

    public Cluster createCluster(Topic groupDestination) throws ClusterException, JMSException {
        return new RestartableCluster(_factory, groupDestination);
    }

    public Cluster createCluster(String topic) throws ClusterException, JMSException {
        return new RestartableCluster(_factory, topic);
    }

}
