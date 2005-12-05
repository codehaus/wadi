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

import org.codehaus.wadi.Motable;
import org.codehaus.wadi.OldMessage;
import org.codehaus.wadi.ProxiedLocation;

public class RelocationResponse implements OldMessage, Serializable {
	
	protected String _sessionName;
	protected String _nodeName;
	protected Motable _motable;
	protected ProxiedLocation _location;
	
	// use when relocating session...
	public RelocationResponse(String sessionName, String nodeName, Motable motable) {
		_sessionName=sessionName;
		_nodeName=nodeName;
		_motable=motable;
		_location=null;
	}
	
	// use when relocating request...
	public RelocationResponse(String sessionName, String nodeName, ProxiedLocation location) {
		_sessionName=sessionName;
		_nodeName=nodeName;
		_motable=null;
		_location=location;
	}
	
	// use when session was not found...
	public RelocationResponse(String sessionName) {
		_sessionName=sessionName;
		_nodeName=null;
		_motable=null;
		_location=null;
	}
	
	protected RelocationResponse() {
		// for deserialising
	}
	
	public String getSessionName() {
		return _sessionName;
	}
	
	public String getNodeName() {
		return _nodeName;
	}
	
	public Motable getMotable() {
		return _motable;
	}
	
	public ProxiedLocation getProxiedLocation() {
		return _location;
	}
	
	public String toString() {
		return "<RelocationResponse: "+_sessionName+" -> "+_nodeName+": "+(_motable!=null?"session":_location!=null?"request":"failed")+">";
	}
	
}
