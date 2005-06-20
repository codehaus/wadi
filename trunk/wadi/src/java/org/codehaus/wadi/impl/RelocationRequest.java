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
package org.codehaus.wadi.impl;

import java.io.Serializable;

import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.dindex.impl.AbstractDIndexRequest;

public class RelocationRequest extends AbstractDIndexRequest implements Serializable {

    protected static final boolean _RELOCATE_SESSION_PREFERRED=true;
    protected static final boolean _RELOCATE_REQUEST_PREFERRED=false;
    
    protected long _timeSent=System.currentTimeMillis();

    private String _sessionName;
    private String _nodeName;
    private boolean _sessionOrRequestPreferred; // t=session, f=request
    private boolean _shuttingDown;
    //protected boolean _acceptingSessions;
    private long _lastKnownTime;
    private String _lastKnownPlace;
    private long _requestHandOverTimeout;
    
    public RelocationRequest(String sessionName, String nodeName, boolean sessionOrRequestPreferred, boolean shuttingDown, long lastKnownTime, String lastKnownPlace, long requestHandOverTimeout) {
        super(sessionName);
        _sessionName=sessionName;
        _nodeName=nodeName;
        _sessionOrRequestPreferred=sessionOrRequestPreferred;
        _shuttingDown=shuttingDown;
        _lastKnownTime=lastKnownTime;
        _lastKnownPlace=lastKnownPlace;
        _requestHandOverTimeout=requestHandOverTimeout;
    }
    
    protected RelocationRequest() {
        // used when deserialising
    }

    public String getSessionName() {
        return _sessionName;
    }
    
    public String getNodeName() {
        return _nodeName;
    }

    public boolean getSessionOrRequestPreferred() {
        return _sessionOrRequestPreferred;
    }

    public boolean getShuttingDown() {
        return _shuttingDown;
    }

    public long getLastKnownTime() {
        return _lastKnownTime;
    }

    public String getLastKnownPlace() {
        return _lastKnownPlace;
    }

    public long getRequestHandOverTimeout() {
        return _requestHandOverTimeout;
    }
    
}
