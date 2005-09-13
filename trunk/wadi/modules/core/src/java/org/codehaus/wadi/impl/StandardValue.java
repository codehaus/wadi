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

import org.codehaus.wadi.Value;
import org.codehaus.wadi.ValueConfig;

/**
 * Allows [de]serialisation of its content via optional pluggable Helper class.
 * This allows us to deal with the special cases mentioned in J2EE.6.4 in a non
 * app-server specific manner. In other words, we can deal with attributes that
 * are non-serialisable, provided that the application writer provides a mechanism
 * for their persistance. Types such as EJBHome, EJBObject etc. are likely to be placed
 * into distributable Sessions.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class StandardValue implements Value {
    
    protected final ValueConfig _config;
    
    public StandardValue(ValueConfig config) {_config=config;}
    
    protected Object _value;
    
    public Object getValue() {
        return _value;
    }
    
    public Object setValue(Object newValue) {
        Object oldValue=_value;
        _value=newValue;
        return oldValue;
    }	
}
