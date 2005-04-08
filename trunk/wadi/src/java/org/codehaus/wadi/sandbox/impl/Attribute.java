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
import java.util.ArrayList;
import java.util.List;

import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.sandbox.AttributeHelper;

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

public class Attribute implements SerializableContent {
    
    protected final Session _session;

    public Attribute(Session session) {_session=session;}
    
    protected Object _value;
    
    public Object getValue() {
        return _value;
    }
    
    public Object setValue(Object newValue) {
        Object oldValue=_value;
        _value=newValue;
        return oldValue;
    }
    
    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        _value=oi.readObject();
    }

    public void writeContent(ObjectOutput oo) throws IOException {
        AttributeHelper helper=(_value==null || _value instanceof Serializable)?null:findHelper(_value.getClass());
        Object value=(helper==null?_value:helper.replace(_value));
        oo.writeObject(value);        
    }
    
    //--------------------------------------------------
    // static stuff... - yeugh ! - but the thought of chaining backptrs all the way
    // up to the manager is horribly expensive...
    
    // TODO - change from storing List in static to storing it on Manager and having Session push 
    // it into a ThreadLocal before every Serialisation, so that all the Sessions attributes have access
    // to it when they need it.
    
    protected static List _helpers=new ArrayList();
    
    static class HelperPair {
        
        final Class _type;
        final AttributeHelper _helper;

        HelperPair(Class type, AttributeHelper helper) {
            _type=type;
            _helper=helper;
        }
    }
    
    /**
     * Register an AttributeHelper for a particular type. During [de]serialisation
     * Objects flowing in/out of the persistance medium will be passed through this
     * Helper, which will have the opportunity to convert them between Serializable
     * and non-Serializable representations. Helpers will be returned in their registration
     * order, so this is significant (as an Object may implement more than one interface
     * or registered type).
     * 
     * @param type
     * @param helper
     */
    public static void registerHelper(Class type, AttributeHelper helper) {
        _helpers.add(new HelperPair(type, helper));
    }
    
    public static boolean deregisterHelper(Class type) {
        int l=_helpers.size();
        for (int i=0; i<l; i++)
            if (type.equals(((HelperPair)_helpers.get(i))._type)) {
                _helpers.remove(i);
                return true;
            }
        return false;
    }
    
    public static AttributeHelper findHelper(Class type) {
        int l=_helpers.size();
        for (int i=0; i<l; i++) {
            HelperPair p=(HelperPair)_helpers.get(i);
            if (p._type.isAssignableFrom(type))
                return p._helper;
        }
        return null;
    }
}
