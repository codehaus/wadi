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
package org.codehaus.wadi.location.statemanager;


import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.servicespace.ServiceName;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 2276 $
 */
public interface StateManager extends Lifecycle {
    ServiceName NAME = new ServiceName("StateManager");
    
    boolean offerEmigrant(Motable emotable, ReplicaInfo replicaInfo);

    boolean insert(Object id);

    void remove(Object id);

    void relocate(Object id);
}
