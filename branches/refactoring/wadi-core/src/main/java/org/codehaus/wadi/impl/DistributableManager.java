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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.ReplicaterFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.ReplicableSessionConfig;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.WebSessionWrapperFactory;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * A StandardManager that knows how to Serialise HttpSessions.
 */
public class DistributableManager extends StandardManager implements ReplicableSessionConfig {
	
	protected final List _helpers = new ArrayList();
    protected final SynchronizedBoolean shuttingDown = new SynchronizedBoolean(false);
    protected final Streamer _streamer;
    protected final boolean _accessOnLoad;
    protected final ReplicaterFactory _replicaterFactory;

    public DistributableManager(WebSessionPool sessionPool,
            AttributesFactory attributesFactory,
            ValuePool valuePool,
            WebSessionWrapperFactory sessionWrapperFactory,
            SessionIdFactory sessionIdFactory,
            Contextualiser contextualiser,
            ConcurrentMotableMap map,
            Router router,
            boolean errorIfSessionNotAcquired,
            Streamer streamer,
            boolean accessOnLoad,
            ReplicaterFactory replicaterFactory) {
        super(sessionPool,
                attributesFactory,
                valuePool,
                sessionWrapperFactory,
                sessionIdFactory,
                contextualiser,
                map,
                router,
                errorIfSessionNotAcquired);
        _streamer = streamer;
        _accessOnLoad = accessOnLoad;
        _replicaterFactory = replicaterFactory;
    }

    /**
     * Register a ValueHelper for a particular type. During [de]serialisation
     * Objects flowing in/out of the persistance medium will be passed through
     * this Helper, which will have the opportunity to convert them between
     * Serializable and non-Serializable representations. Helpers will be
     * returned in their registration order, so this is significant (as an
     * Object may implement more than one interface or registered type).
     * 
     * @param type
     * @param helper
     */
    public void registerHelper(Class type, ValueHelper helper) {
        _helpers.add(new HelperPair(type, helper));
    }

    public boolean deregisterHelper(Class type) {
        int l = _helpers.size();
        for (int i = 0; i < l; i++)
            if (type.equals(((HelperPair) _helpers.get(i))._type)) {
                _helpers.remove(i);
                return true;
            }
        return false;
    }

    public ValueHelper findHelper(Class type) {
        int l = _helpers.size();
        for (int i = 0; i < l; i++) {
            HelperPair p = (HelperPair) _helpers.get(i);
            if (p._type.isAssignableFrom(type))
                return p._helper;
        }
        return null;
    }

    static class HelperPair {
        final Class _type;

        final ValueHelper _helper;

        HelperPair(Class type, ValueHelper helper) {
            _type = type;
            _helper = helper;
        }
    }

    public boolean getHttpSessionAttributeListenersRegistered() {
        return _attributeListeners.length > 0;
    }

    public boolean getDistributable() {
        return true;
    }

    public Streamer getStreamer() {
        return _streamer;
    }

    public Replicater getReplicater() {
        return _replicaterFactory.create(this);
    }

}
