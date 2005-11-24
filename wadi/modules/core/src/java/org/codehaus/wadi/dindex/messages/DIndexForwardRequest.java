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
package org.codehaus.wadi.dindex.messages;

import java.io.Serializable;

import org.codehaus.wadi.dindex.DIndexRequest;

public class DIndexForwardRequest implements DIndexRequest, Serializable {

    protected DIndexRequest _request;

    public DIndexForwardRequest(DIndexRequest request) {
        _request=request;
    }

    protected DIndexForwardRequest() {
        // for deserialisation...
    }

    public DIndexRequest getRequest() {
        return _request;
    }

    public int getPartitionKey(int numPartitions) {
        return _request.getPartitionKey(numPartitions);
    }

    public String getName() {
        return _request.getName();
    }

    public String toString() {
        return "["+_request.toString()+"]";
    }
}