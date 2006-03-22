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
import org.codehaus.wadi.replication.message.ResultInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;


/**
 * 
 * @version $Revision$
 */
class RetrieveReplicaInfoRequest extends AbstractTwoWayStorageRequest {
    private static final long serialVersionUID = -967961921827664024L;

    public RetrieveReplicaInfoRequest(Object key) {
        super(key);
    }

    public ResultInfo executeWithResult(ReplicaStorage storage) {
        boolean storeReplicaInfo = storage.storeReplicaInfo(key);
        
        ResultInfo resultInfo;
        if (storeReplicaInfo) {
            ReplicaInfo info = storage.retrieveReplicaInfo(key);
            resultInfo = new ResultInfo(info, true);
        } else {
            resultInfo = new ResultInfo(null, false);
        }
        return resultInfo;
    }
}
