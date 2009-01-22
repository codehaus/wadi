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
package org.codehaus.wadi.core.session;


/**
 * A simple slot for holding and Attribute's value
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1177 $
 */
public class StandardValue implements Value {
    
    protected Object value;

    public Object getValue() {
        return value;
    }

    public Object setValue(Object newValue) {
        Object oldValue = value;
        value = newValue;
        return oldValue;
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof StandardValue)) {
            return false;
        }
        StandardValue other = (StandardValue) obj;
        if (null == value) {
            return null == other.value; 
        }
        return value.equals(other.value);
    }

    public int hashCode() {
        return null == value ? 1 : value.hashCode();
    }
    
}
