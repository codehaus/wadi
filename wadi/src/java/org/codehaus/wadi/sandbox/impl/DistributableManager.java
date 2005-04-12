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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.AttributesPool;
import org.codehaus.wadi.sandbox.Dirtier;
import org.codehaus.wadi.sandbox.DistributableSessionConfig;
import org.codehaus.wadi.sandbox.SessionPool;
import org.codehaus.wadi.sandbox.ValueHelper;
import org.codehaus.wadi.sandbox.ValuePool;

public class DistributableManager extends Manager implements DistributableSessionConfig {

    protected final StreamingStrategy _streamer;
    protected final Dirtier _dirtier;

    public DistributableManager(SessionPool sessionPool, AttributesPool attributesPool, ValuePool valuePool, StreamingStrategy streamer, Dirtier dirtier) {
        super(sessionPool, attributesPool, valuePool);
        _streamer=streamer;
        _dirtier=dirtier;
    }

    // Distributable
    public StreamingStrategy getStreamer() {return _streamer;}
    public Dirtier getDirtier() {return _dirtier;}
    
    
    
    
    //--------------------------------------------------
    // static stuff... - yeugh ! - but the thought of chaining backptrs all the way
    // up to the manager is horribly expensive...
    
    // TODO - change from storing List in static to storing it on Manager and having Session push 
    // it into a ThreadLocal before every Serialisation, so that all the Sessions attributes have access
    // to it when they need it.
    
    protected final List _helpers=new ArrayList();
    
    static class HelperPair {
        
        final Class _type;
        final ValueHelper _helper;

        HelperPair(Class type, ValueHelper helper) {
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
    public void registerHelper(Class type, ValueHelper helper) {
        _helpers.add(new HelperPair(type, helper));
    }
    
    public boolean deregisterHelper(Class type) {
        int l=_helpers.size();
        for (int i=0; i<l; i++)
            if (type.equals(((HelperPair)_helpers.get(i))._type)) {
                _helpers.remove(i);
                return true;
            }
        return false;
    }
    
    public ValueHelper findHelper(Class type) {
        int l=_helpers.size();
        for (int i=0; i<l; i++) {
            HelperPair p=(HelperPair)_helpers.get(i);
            if (p._type.isAssignableFrom(type))
                return p._helper;
        }
        return null;
    }
    
}
