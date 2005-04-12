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

import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.sandbox.DistributableValueConfig;
import org.codehaus.wadi.sandbox.ValueHelper;

/**
 * An AttributeWrapper that supports the lazy notification of HttpSessionActivationListeners.
 * Listeners are notified lazily, as their activation may be an expensive and unnecessary
 * operation.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class DistributableValue extends StandardValue implements SerializableContent  {
    
    public DistributableValue(DistributableValueConfig config) {super(config);}
    
    protected transient boolean _needsNotification;
    
    public synchronized Object getValue() {
        if (_needsNotification) {
            ((HttpSessionActivationListener)_value).sessionDidActivate(_config==null?null:((DistributableValueConfig)_config).getHttpSessionEvent());
            _needsNotification=false;
        }
        return super.getValue();
    }
    
    public synchronized Object setValue(Object newValue) {
        if (newValue!=null && !(newValue instanceof Serializable) && ((DistributableValueConfig)_config).findHelper(newValue.getClass())==null)
            throw new IllegalArgumentException("Distributable HttpSession attribute values must be Serializable or of other designated type (see SRV.7.7.2)");

        if (_needsNotification) {
            // as _value is about to be unbound, it should be activated first...
            // IDEA - if it is not a BindingListener and no AttributeListeners are
            // registered, do we need to do this ? I think we should still probably do it...
            ((HttpSessionActivationListener)_value).sessionDidActivate(_config==null?null:((DistributableValueConfig)_config).getHttpSessionEvent());
            _needsNotification=false;
        }
        return super.setValue(newValue);
    }
     
    public synchronized void writeContent(ObjectOutput oo) throws IOException {
        if (_value instanceof HttpSessionActivationListener) {
            ((HttpSessionActivationListener)_value).sessionWillPassivate(_config==null?null:((DistributableValueConfig)_config).getHttpSessionEvent());
            _needsNotification=true;
        }

        ValueHelper helper=(_value==null || _value instanceof Serializable)?null:((DistributableValueConfig)_config).findHelper(_value.getClass());
        Object value=(helper==null?_value:helper.replace(_value));
        oo.writeObject(value);        
    }
    
    public synchronized void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        _value=oi.readObject();
        _needsNotification=(_value instanceof HttpSessionActivationListener);
    }

}
