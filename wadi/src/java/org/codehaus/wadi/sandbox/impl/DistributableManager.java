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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.wadi.IdGenerator;
import org.codehaus.wadi.RoutingStrategy;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.AttributesPool;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.DistributableSessionConfig;
import org.codehaus.wadi.sandbox.Router;
import org.codehaus.wadi.sandbox.Session;
import org.codehaus.wadi.sandbox.SessionPool;
import org.codehaus.wadi.sandbox.SessionWrapperFactory;
import org.codehaus.wadi.sandbox.ValueHelper;
import org.codehaus.wadi.sandbox.ValuePool;

public class DistributableManager extends Manager implements DistributableSessionConfig {

    protected final StreamingStrategy _streamer;

    public DistributableManager(SessionPool sessionPool, AttributesPool attributesPool, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, IdGenerator sessionIdFactory, Contextualiser contextualiser, Map sessionMap, Router router, StreamingStrategy streamer, boolean accessOnLoad) {
        super(sessionPool, attributesPool, valuePool, sessionWrapperFactory, sessionIdFactory, contextualiser, sessionMap, router, accessOnLoad);
        _streamer=streamer;
    }

    // Distributable
    public StreamingStrategy getStreamer() {return _streamer;}

    static class HelperPair {

        final Class _type;
        final ValueHelper _helper;

        HelperPair(Class type, ValueHelper helper) {
            _type=type;
            _helper=helper;
        }
    }

    protected final List _helpers=new ArrayList();

    /**
     * Register a ValueHelper for a particular type. During [de]serialisation
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

    public void destroySession(Session session) {
        // this destroySession method must not chain the one in super - otherwise the
        // notification aspect fires twice - once around each invocation... - DOH !
        Collection names=new ArrayList((_attributeListeners.size()>0)?(Collection)session.getAttributeNameSet():((DistributableSession)session).getListenerNames());
        for (Iterator i=names.iterator(); i.hasNext();) // ALLOC ?
            session.removeAttribute((String)i.next());

        // TODO - remove from Contextualiser....at end of initial request ? Think more about this
        String id=session.getId();
        _map.remove(id);
        session.destroy();
        _sessionPool.put(session);
        if (_log.isDebugEnabled()) _log.debug("destroyed: "+id);
    }

    // Lazy

    public boolean getHttpSessionAttributeListenersRegistered(){return _attributeListeners.size()>0;}

    public boolean getDistributable(){return true;}

}
