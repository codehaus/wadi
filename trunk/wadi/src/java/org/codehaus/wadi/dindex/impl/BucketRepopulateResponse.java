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
package org.codehaus.wadi.dindex.impl;

import java.io.Serializable;
import java.util.Collection;

public class BucketRepopulateResponse implements Serializable {

    protected Collection[] _keys;
    
    public BucketRepopulateResponse(Collection[] keys) {
        super();
        _keys=keys;
    }

    protected BucketRepopulateResponse() {
        // for deserialisation
    }
    
    public Collection[] getKeys() {
        return _keys;
    }
    
	
    public String toString() {
    	StringBuffer buffer=new StringBuffer("<BucketRepopulateResponse: ");
    	for (int i=0; i<_keys.length; i++) {
    		Collection c=_keys[i];
    		if (c!=null)
    			buffer.append(""+i+":"+c.toString()+", ");
    	}
    	buffer.append(">");
    	return buffer.toString();
    }
}
