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
package org.codehaus.wadi.location.newmessages;

import java.io.Serializable;

import org.codehaus.wadi.location.impl.AbstractDIndexRequest;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class MoveIMToPM extends AbstractDIndexRequest implements Serializable {

    protected String _peerName;
    protected boolean _shuttingDown;
    protected boolean _invocationIsRelocatable;

    public MoveIMToPM(String sessionName, String peerName, boolean shuttingDown, boolean invocationIsRelocatable) {
        super(sessionName);
        _peerName=peerName;
        _shuttingDown=shuttingDown;
        _invocationIsRelocatable=invocationIsRelocatable;
    }

    public String getNodeName() {
    	return _peerName;
    }

    public boolean getShuttingDown() {
        return _shuttingDown;
    }

    public boolean getInvocationIsRelocatable() {
        return _invocationIsRelocatable;
    }
    
    public String toString() {
        return "<MoveIMToPM:"+_sessionKey+"->"+_peerName+">";
    }

}
