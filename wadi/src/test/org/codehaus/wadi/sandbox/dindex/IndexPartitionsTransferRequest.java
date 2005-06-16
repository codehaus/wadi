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
package org.codehaus.wadi.sandbox.dindex;

import java.io.Serializable;

public class IndexPartitionsTransferRequest implements Serializable {

  protected long _timeStamp;
    protected LocalBucket[] _buckets;

  public IndexPartitionsTransferRequest(long timeStamp, LocalBucket[] buckets) {
    _timeStamp=timeStamp;
    _buckets=buckets;
  }

    protected IndexPartitionsTransferRequest() {
        // for deserialisation
    }

  public long getTimeStamp() {
    return _timeStamp;
  }

    public LocalBucket[] getBuckets() {
        return _buckets;
    }

}
