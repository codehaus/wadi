package org.codehaus.wadi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.DistributableContextualiserConfig;
import org.codehaus.wadi.DistributableSessionConfig;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValueHelper;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.ClusteredManager.HelperPair;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * @author jules
 * A StandardManager thatknows how to Serialise HttpSessions.
 */
public class DistributableManager extends StandardManager implements DistributableSessionConfig, DistributableContextualiserConfig {

	protected final List _helpers = new ArrayList();
	protected final SynchronizedBoolean _shuttingDown = new SynchronizedBoolean(false);
    protected final Streamer _streamer;
	protected final boolean _accessOnLoad;

	public DistributableManager(SessionPool sessionPool, AttributesFactory attributesFactory, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, SessionIdFactory sessionIdFactory, Contextualiser contextualiser, Map map, Router router, Streamer streamer, boolean accessOnLoad) {
    	super(sessionPool, attributesFactory, valuePool, sessionWrapperFactory, sessionIdFactory, contextualiser, map, router);
    	_streamer=streamer;
    	_accessOnLoad=accessOnLoad;
    }

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

	public boolean getHttpSessionAttributeListenersRegistered() {
		return _attributeListeners.length>0;
	}

	public boolean getDistributable() {
		return true;
	}

	public boolean getAccessOnLoad() {
	    return _accessOnLoad;
	}

	public SynchronizedBoolean getShuttingDown() {
	    return _shuttingDown;
	}

	public Streamer getStreamer() {return _streamer;}
	
}
