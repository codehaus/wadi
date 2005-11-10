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
package org.codehaus.wadi.gridstate.messages;

import java.io.Serializable;

import javax.jms.Destination;

public class WriteIMToPM implements Serializable {

	protected Object _key;
	protected boolean _valueIsNull;
	protected boolean _overwrite;
	protected boolean _returnOldValue;
	protected Destination _im;

	public WriteIMToPM(Object key, boolean valueIsNull, boolean overwrite, boolean returnOldValue, Destination im) {
		_key=key;
		_valueIsNull=valueIsNull;
		_overwrite=overwrite;
		_returnOldValue=returnOldValue;
		_im=im;
	}

	protected WriteIMToPM() {
		// for deserialisation...
	}

	public Object getKey() {
		return _key;
	}

	public boolean getValueIsNull() {
		return _valueIsNull;
	}

	public boolean getOverwrite() {
		return _overwrite;
	}

	public boolean getReturnOldValue() {
		return _returnOldValue;
	}

	public Destination getIM() {
		return _im;
	}

}
