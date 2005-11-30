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

public class MoveSMToIM implements Serializable {

	protected Object _key; // do we really need the key - TODO
	protected Object _value; // should be strictly typed to byte[] - TODO

	public MoveSMToIM(Object key, Object value) {
		_key=key;
		_value=value;
	}

	protected MoveSMToIM() {
		// for deserialisation...
	}

	public Object getKey() {
		return _key;
	}

	public Object getValue() {
		return _value;
	}
	
	public String toString() {
		byte[] bytes=(byte[])_value;
		int l=(bytes==null?0:bytes.length);
		return "<MoveSMToIM:"+_key+":"+l+"bytes>";
	}

}
