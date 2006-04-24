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

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.dindex.Partition;
import org.codehaus.wadi.dindex.PartitionConfig;
import org.codehaus.wadi.dindex.newmessages.DeleteIMToPM;
import org.codehaus.wadi.dindex.newmessages.EvacuateIMToPM;
import org.codehaus.wadi.dindex.newmessages.InsertIMToPM;
import org.codehaus.wadi.dindex.newmessages.MoveIMToPM;
import org.codehaus.wadi.group.Dispatcher;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class PartitionFacade extends AbstractPartition {


    protected final ReadWriteLock _lock=new WriterPreferenceReadWriteLock();
    protected final LinkedQueue _queue=new LinkedQueue();
    protected final PartitionConfig _config;
    protected final Log _log;

    protected long _timeStamp;
    protected Partition _content;

    public PartitionFacade(int key, long timeStamp, Partition content, boolean queueing, PartitionConfig config) {
        super(key);
        _config=config;
        _timeStamp=timeStamp;
        _log=LogFactory.getLog(getClass().getName()+"#"+_key+"@"+_config.getLocalNodeName());
        if (content instanceof UnknownPartition) {
        	try {
        		_lock.writeLock().acquire();
        	} catch (InterruptedException e) {
        		_log.error("lock acquisition interrupted - NYI", e);
        		throw new UnsupportedOperationException(e.getMessage());
        	}
        }
        _content=content;
        if (_log.isTraceEnabled()) _log.trace("initialising location to: "+_content);
    }

    public boolean isLocal() { // locking ?
    	if (_content instanceof UnknownPartition)
    		return false;
    	
        Sync sync=_lock.writeLock(); // EXCLUSIVE
        boolean acquired=false;
        try {
            sync.acquire();
            acquired=true;
            return _content.isLocal();
        } catch (InterruptedException e) {
        	_log.warn("unexpected problem", e);
        } finally {
            if (acquired)
                sync.release();
        }
        throw new UnsupportedOperationException();
    }

    public Partition getContent() {
        Sync sync=_lock.writeLock(); // EXCLUSIVE
        boolean acquired=false;
        try {
            sync.acquire();
            acquired=true;
            return _content;
        } catch (InterruptedException e) {
                _log.warn("unexpected problem", e);
        } finally {
            if (acquired)
                sync.release();
        }
        throw new UnsupportedOperationException();
    }

    public void setContent(long timeStamp, Partition content) {
    	// TODO - do something here
    		Sync sync=_lock.writeLock(); // EXCLUSIVE
    		boolean acquired=false;
    		try {
    			if (!(_content instanceof UnknownPartition))
    				sync.acquire();
    			acquired=true;
    			if (timeStamp>_timeStamp) {
    				if (_log.isTraceEnabled()) _log.trace("["+_key+"] changing location from: "+_content+" to: "+content);
    				_timeStamp=timeStamp;
    				_content=content;
    			}

    		} catch (InterruptedException e) {
    			_log.warn("unexpected problem", e);
    		} finally {
    			if (acquired && !(_content instanceof UnknownPartition))
    				sync.release();
    		}
    	}

    public void setContentRemote(long timeStamp, Dispatcher dispatcher, Destination location) {
    	Sync sync=_lock.writeLock(); // EXCLUSIVE
    	boolean acquired=false;
    	try {
    		if (!(_content instanceof UnknownPartition))
    			sync.acquire();
    		acquired=true;
    		if (timeStamp>_timeStamp) {
    			_timeStamp=timeStamp;
    			if (_content instanceof RemotePartition) {
    				((RemotePartition)_content).setLocation(location);
    			} else {
    				if (_log.isTraceEnabled()) _log.trace("["+_key+"] changing location from: "+_content+" to: "+_config.getNodeName(location));
    				_content=new RemotePartition(_key, _config, location);
    			}
    		}
    	} catch (InterruptedException e) {
    		_log.warn("unexpected problem", e);
    	} finally {
    		if (acquired && !(_content instanceof UnknownPartition))
    			sync.release();
    	}
    }

    public ObjectMessage exchange(DIndexRequest request, long timeout) throws Exception {
    	if (_log.isTraceEnabled()) _log.trace("dispatching: "+request+" on "+_content);
    	Sync sync=_lock.readLock(); // SHARED
    	boolean acquired=false;
    	try {
    		sync.acquire();
    		acquired=true;
        	return _content.exchange(request, timeout);
    	} catch (InterruptedException e) {
    		_log.warn("unexpected problem", e);
    		return null;
    	} finally {
    		if (acquired)
    			sync.release();
    	}	
    }

    public void onMessage(ObjectMessage message, InsertIMToPM request) {
    	if (_log.isTraceEnabled()) _log.trace("dispatching: "+request+" on "+_content);
    	Sync sync=_lock.readLock(); // SHARED
    	boolean acquired=false;
    	try {
    		sync.acquire();
    		acquired=true;
    		_content.onMessage(message, request);
    	} catch (InterruptedException e) {
    		_log.warn("unexpected problem", e);
    	} finally {
    		if (acquired)
    			sync.release();
    	}
    }

    public void onMessage(ObjectMessage message, DeleteIMToPM request) {
    	Sync sync=_lock.readLock(); // SHARED
    	boolean acquired=false;
    	try {
    		sync.acquire();
    		acquired=true;
    		_content.onMessage(message, request);
    	} catch (InterruptedException e) {
    		_log.warn("unexpected problem", e);
    	} finally {
    		if (acquired)
    			sync.release();
    	}
    }

    public void onMessage(ObjectMessage message, EvacuateIMToPM request) {
    	Sync sync=_lock.readLock(); // SHARED
    	boolean acquired=false;
    	try {
    		sync.acquire();
    		acquired=true;
    		_content.onMessage(message, request);
    	} catch (InterruptedException e) {
    		_log.warn("unexpected problem", e);
    	} finally {
    		if (acquired)
    			sync.release();
    	}
    }

    // should superceded above method
    public void onMessage(ObjectMessage message, MoveIMToPM request) {
    	Sync sync=_lock.readLock(); // SHARED
    	boolean acquired=false;
    	try {
    		sync.acquire();
    		acquired=true;
    		_content.onMessage(message, request);
    	} catch (InterruptedException e) {
    		_log.warn("unexpected problem", e);
    	} finally {
    		if (acquired)
    			sync.release();
    	}
    }

}