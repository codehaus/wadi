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
package org.codehaus.wadi.location.impl;

import java.io.Serializable;

import org.codehaus.wadi.location.SessionRequestMessage;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1924 $
 */
public abstract class SessionRequestImpl implements SessionRequestMessage, Serializable {
    protected final Object _key;
    private int version;

    public SessionRequestImpl(Object key) {
        if (null == key) {
            throw new IllegalArgumentException("key is required");
        }
        _key = key;
    }

    public Object getKey() {
        return _key;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }
    
}
