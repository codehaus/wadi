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
package org.codehaus.wadi.location.session;

import java.io.Serializable;

import org.codehaus.wadi.location.impl.SessionRequestImpl;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class MoveIMToPM extends SessionRequestImpl implements Serializable {

    protected String _peerName;
    protected boolean _relocateSession;
    protected boolean _relocateInvocation;

    public MoveIMToPM(String sessionName, String peerName, boolean relocateSession, boolean relocateInvocation) {
        super(sessionName);
        _peerName=peerName;
        _relocateSession=relocateSession;
        _relocateInvocation=relocateInvocation;
    }

    public String getNodeName() {
    	return _peerName;
    }

    public boolean getRelocateSession() {
        return _relocateSession;
    }

    public boolean getRelocateInvocation() {
        return _relocateInvocation;
    }
    
    public String toString() {
        return "<MoveIMToPM:"+_key+"->"+_peerName+">";
    }

}
