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
package org.codehaus.wadi.sandbox.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import javax.servlet.http.HttpSessionActivationListener;

import org.codehaus.wadi.sandbox.DistributableValueConfig;
import org.codehaus.wadi.sandbox.SerializableContent;
import org.codehaus.wadi.sandbox.ValueHelper;

/**
 * An attribute Value that supports the notification of HttpSessionActivationListeners at the correct
 * times as well as the substition of non-Serializable content with the results of pluggable Helpers.
 * It does not expect to be accessed after serialisation, until a fresh deserialisation has occurred.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class DistributableValue extends StandardValue implements SerializableContent  {
    
    public DistributableValue(DistributableValueConfig config) {super(config);}
    
    protected ValueHelper _helper;
    
    public synchronized Object setValue(Object newValue) {
        if (newValue!=null && !(newValue instanceof Serializable) && (_helper=((DistributableValueConfig)_config).findHelper(newValue.getClass()))==null)
            throw new IllegalArgumentException("Distributable HttpSession attribute values must be Serializable or of other designated type (see SRV.7.7.2)");

        return super.setValue(newValue);
    }
     
    public synchronized void writeContent(ObjectOutput oo) throws IOException {
        if (_value!=null && _value instanceof HttpSessionActivationListener) {
            ((HttpSessionActivationListener)_value).sessionWillPassivate(_config==null?null:((DistributableValueConfig)_config).getHttpSessionEvent());
        }

        Object value=(_helper==null?_value:_helper.replace(_value));
        oo.writeObject(value);
    }
    
    public synchronized void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        if ((_value=_value=oi.readObject())!=null && _value instanceof HttpSessionActivationListener) {
            ((HttpSessionActivationListener)_value).sessionDidActivate(_config==null?null:((DistributableValueConfig)_config).getHttpSessionEvent());
        }
        
        if (_value!=null && !(_value instanceof Serializable))
            _helper=((DistributableValueConfig)_config).findHelper(_value.getClass());
    }

}
