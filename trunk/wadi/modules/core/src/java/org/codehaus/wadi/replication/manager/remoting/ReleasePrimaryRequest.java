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
package org.codehaus.wadi.replication.manager.remoting;

import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.message.AbstractTwoWayMessage;
import org.codehaus.wadi.replication.message.ResultInfo;

/**
 * 
 * @version $Revision$
 */
class ReleasePrimaryRequest extends AbstractTwoWayMessage implements ReplicationManagerRequest {
    private static final long serialVersionUID = -235700840340515049L;

    private final Object key;
    
    public ReleasePrimaryRequest(Object key) {
        this.key = key;
    }
    
    public ResultInfo executeWithResult(ReplicationManager manager) {
        boolean managePrimary = manager.managePrimary(key);
        ReplicaInfo info = null;
        boolean replyWithResult = false;
        if (managePrimary) {
            info = manager.releasePrimary(key);
            replyWithResult = true;
        }
        return new ResultInfo(info, replyWithResult);
    }
    
    public void execute(ReplicationManager manager) {
        throw new UnsupportedOperationException();
    }
}