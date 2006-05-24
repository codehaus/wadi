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
package org.codehaus.wadi.dindex.newmessages;

import java.io.Serializable;

import org.codehaus.wadi.OldMessage;
import org.codehaus.wadi.dindex.impl.AbstractDIndexRequest;

public class MoveIMToPM extends AbstractDIndexRequest implements OldMessage, Serializable {

    private String _nodeName;
    private int _numConcurrentInvocations;
    private boolean _shuttingDown;
    
    public MoveIMToPM(String sessionName, String nodeName, int numConcurrentInvocations, boolean shuttingDown) {
        super(sessionName);
        _nodeName=nodeName;
        _numConcurrentInvocations=numConcurrentInvocations;
        _shuttingDown=shuttingDown;
    }
    
    protected MoveIMToPM() {
        // used when deserialising
    }

    public String getNodeName() {
    	return _nodeName;
    }
    
    public int getNumConcurrentInvocations() {
    	return _numConcurrentInvocations;
    }
    
    public boolean getShuttingDown() {
        return _shuttingDown;
    }

    public String toString() {
        return "<MoveIMToPM:"+_key+"->"+_nodeName+">";
    }
    
}