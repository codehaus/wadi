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
package org.codehaus.wadi.replication.storage.remoting;

import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;


/**
 * 
 * @version $Revision$
 */
class UpdateRequest extends AbstractOneWayStorageRequest {
    private static final long serialVersionUID = -8106353766595506032L;

    private final ReplicaInfo replicaInfo;
    
    public UpdateRequest(Object key, ReplicaInfo replicaInfo) {
        super(key);
        this.replicaInfo = replicaInfo;
    }

    public void execute(ReplicaStorage storage) {
        storage.mergeUpdate(key, replicaInfo);
    }
}
