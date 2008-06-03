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
package org.codehaus.wadi.group;

import java.io.Serializable;

/**
 * 
 * @version $Revision: $
 */
public class PeerInfo implements Serializable {
    private final long birthtime;
    private final EndPoint endPoint; // TODO - should probably be a Map of EndPoints keyed by Tier

    public PeerInfo(EndPoint endPoint) {
        this(endPoint, System.currentTimeMillis());
    }

    public PeerInfo(EndPoint endPoint, long birthtime) {
        this.birthtime = birthtime;
        this.endPoint=endPoint;
    }
    
    public long getBirthtime() {
        return birthtime;
    }
    
    public EndPoint getEndPoint() {
    	return endPoint;
    }
    
}
