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

import javax.servlet.http.HttpSessionActivationListener;

/**
 * An AttributeWrapper that supports the lazy notification of HttpSessionActivationListeners.
 * Listeners are notified lazily, as their activation may be an expensive and unnecessary
 * operation.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class ActivatableAttribute extends Attribute {
    
    public ActivatableAttribute(Session session) {super(session);}
    
    protected transient boolean _needsNotification;
    
    public synchronized Object getValue() {
        if (_needsNotification) {
            ((HttpSessionActivationListener)_value).sessionDidActivate(_session==null?null:_session.getHttpSessionEvent());
            _needsNotification=false;
        }
        return super.getValue();
    }
    
    public synchronized Object setValue(Object newValue) {
        if (_needsNotification) {
            // as _value is about to be unbound, it should be activated first...
            // IDEA - if it is not a BindingListener and no AttributeListeners are
            // registered, do we need to do this ? I think we should still probably do it...
            ((HttpSessionActivationListener)_value).sessionDidActivate(_session==null?null:_session.getHttpSessionEvent());
            _needsNotification=false;
        }
        return super.setValue(newValue);
    }
     
    public synchronized void writeContent(ObjectOutput oo) throws IOException {
        if (_value instanceof HttpSessionActivationListener) {
            ((HttpSessionActivationListener)_value).sessionWillPassivate(_session==null?null:_session.getHttpSessionEvent());
            _needsNotification=true;
        }
        super.writeContent(oo);
    }
    
    public synchronized void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        super.readContent(oi);
        _needsNotification=(_value instanceof HttpSessionActivationListener);
    }
    
}
