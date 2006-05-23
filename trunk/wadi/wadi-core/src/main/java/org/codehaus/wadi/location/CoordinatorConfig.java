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
package org.codehaus.wadi.location;

import java.util.Collection;
import java.util.Map;

import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Peer;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface CoordinatorConfig {

    int getNumPartitions();

    Peer getLocalNode();
    Collection getRemoteNodes();
    Cluster getCluster();
    Dispatcher getDispatcher();

    Map getRendezVousMap();

    Collection getLeavers();
    Collection getLeft();

    long getInactiveTime();
    void regenerateMissingPartitions(Peer[] living, Peer[] leaving);

}
