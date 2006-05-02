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

import org.codehaus.wadi.replication.message.ResultInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;


/**
 * 
 * @version $Revision: 1538 $
 */
class OneWayToTwoWayAdapterRequest extends AbstractTwoWayStorageRequest implements ReplicaStorageRequest {
    private static final long serialVersionUID = -3805587747984934575L;

    private final ReplicaStorageRequest request;
    
    public OneWayToTwoWayAdapterRequest(ReplicaStorageRequest request) {
        super(null);
        this.request = request;
    }

    public ResultInfo executeWithResult(ReplicaStorage storage) {
        request.execute(storage);
        return new ResultInfo(null, true);
    }
    
    public String toString() {
        return "Two-way adapter for " + request;
    }
}
