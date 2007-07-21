/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.replication.storage;

import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.servicespace.ServiceName;


/**
 * 
 * @version $Revision$
 */
public interface ReplicaStorage extends Lifecycle {
    ServiceName NAME = new ServiceName("ReplicaStorage");
    
    void mergeCreate(Object key, ReplicaStorageInfo createStorageInfo) throws ReplicaKeyAlreadyExistsException;

    void mergeUpdate(Object key, ReplicaStorageInfo updateStorageInfo) throws ReplicaKeyAlreadyExistsException;

    void mergeDestroy(Object key);
    
    ReplicaStorageInfo retrieveReplicaStorageInfo(Object key);

    boolean storeReplicaInfo(Object key);
}
