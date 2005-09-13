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
package org.codehaus.wadi.dindex.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.BucketConfig;
import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.dindex.DIndexResponse;
import org.codehaus.wadi.impl.RelocationRequest;
import org.codehaus.wadi.impl.RelocationResponse;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

public class LocalBucket extends AbstractBucket implements Serializable {

    protected static final Log _log = LogFactory.getLog(LocalBucket.class);

    protected Map _map=new ConcurrentHashMap();
    protected transient BucketConfig _config;
    
    public LocalBucket(int key) {
        super(key);
    }

    protected LocalBucket() {
        super();
        // for deserialisation...
    }
    
    public void init(BucketConfig config) {
        _config=config;
    }

    public boolean isLocal() {
        return true;
    }

    public String toString() {
        return "<local:"+_key+">";
    }
    
    public void put(String name, Destination location) {
        _map.put(name, new LockableLocation(location));
    }
    
    public void dispatch(ObjectMessage message, DIndexRequest request) {
    	if (request instanceof DIndexInsertionRequest) {
    		onMessage(message, (DIndexInsertionRequest)request);
    	} else if (request instanceof DIndexDeletionRequest) {
    		onMessage(message, (DIndexDeletionRequest)request);
    	} else if (request instanceof DIndexRelocationRequest) {
    		onMessage(message, (DIndexRelocationRequest)request);
    	} else if (request instanceof DIndexForwardRequest) {
    		onMessage(message, (DIndexForwardRequest)request);
    	} else {
    		_log.info("What should I do with this ?: "+request);
    	}
    }
    
    public static class LockableLocation implements Serializable {

    	public Destination _location;
    	public transient Sync _lock;
    	
    	public LockableLocation(Destination location) {
    		_lock=new Mutex();
    		_location=location;
    	}

    	protected LockableLocation() {
    		_lock=new Mutex();
    	}

    	// prevents Object from being Serialised whilst locked...
    	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    		try {
    			_lock.acquire();
    			out.defaultWriteObject();
    			_lock.release();
    		} catch (Exception e) {
    			_log.error("unexpected problem", e);
    		}
    	}

    }
    
    protected void onMessage(ObjectMessage message, DIndexInsertionRequest request) {
        Destination location=null;
        try{location=message.getJMSReplyTo();} catch (JMSException e) {_log.error("unexpected problem", e);}
        _map.put(request.getName(), new LockableLocation(location)); // remember location of actual session...
        _log.info("insertion {"+request.getName()+" : "+_config.getNodeName(location)+"}");
        DIndexResponse response=new DIndexInsertionResponse();
        // we can optimise local-local send here - TODO
        _config.getDispatcher().reply(message, response);
    }
    
    protected void onMessage(ObjectMessage message, DIndexDeletionRequest request) {
		LockableLocation oldValue=(LockableLocation)_map.remove(request.getName());
		_log.info("delete "+request.getName()+" : "+_config.getNodeName(oldValue._location));
		if (oldValue==null)
			throw new IllegalStateException();
		DIndexResponse response=new DIndexDeletionResponse();
		// we can optimise local-local send here - TODO
		_config.getDispatcher().reply(message, response);
    }
    
    protected void onMessage(ObjectMessage message, DIndexRelocationRequest request) {
        Destination newLocation=null;
        try{newLocation=message.getJMSReplyTo();} catch (JMSException e) {_log.error("unexpected problem", e);}
        LockableLocation ll=(LockableLocation)_map.get(request.getName());
        Destination oldLocation=ll._location;
        ll._location=newLocation;
        _log.info("relocate "+request.getName()+" : "+_config.getNodeName(oldLocation)+" -> "+_config.getNodeName(newLocation));
		_log.info("RELEASING MIGRATION LOCK");
        // TODO - UNLOCK - release session Migration Lock
		ll._lock.release();
        DIndexResponse response=new DIndexRelocationResponse();
        // we can optimise local-local send here - TODO
        _config.getDispatcher().reply(message, response);
    }
    
    protected void onMessage(ObjectMessage message, DIndexForwardRequest request) {
        // we have got to someone who actually knows where we want to go.
        // strip off wrapper and deliver actual request to its final destination...
        String name=request.getName();
        LockableLocation ll=(LockableLocation)_map.get(name);
        if (ll==null) { // session could not be located...
        	DIndexRequest r=request.getRequest();
            if (r instanceof RelocationRequest) {
                assert message!=null;
                assert name!=null;
                assert _config!=null;
                _config.getDispatcher().reply(message, new RelocationResponse(name));
            } else {
                _log.warn("unexpected nested request structure - ignoring: "+r);
            }
        } else { // session succesfully located...
        	assert ll!=null;
        	assert request!=null;
        	assert _config!=null;
        	// TODO - LOCK - acquire Session Migration Lock
        	DIndexRequest r=request.getRequest();
        	if (r instanceof RelocationRequest) {
        		try {
        			_log.info("ACQUIRING MIGRATION LOCK");
        			ll._lock.acquire();
        		} catch (Exception e) {
        			_log.error("unexpected problem, e");
        		}
        	}
        	
        	_log.info("directing: " +request+" -> "+_config.getNodeName(ll._location));
        	if (!_config.getDispatcher().forward(message, ll._location, request.getRequest()))
        		_log.warn("could not forward message");
        }	
    }

}
