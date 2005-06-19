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
package org.codehaus.wadi.sandbox.dindex;

import java.io.Serializable;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.MessageDispatcher;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

public class LocalBucket extends AbstractBucket implements Serializable {

    protected static final Log _log = LogFactory.getLog(LocalBucket.class);

    protected Map _map=new ConcurrentHashMap();
    protected transient MessageDispatcher _dispatcher;

    public LocalBucket(int key) {
        super(key);
    }

    protected LocalBucket() {
        super();
        // for deserialisation...
    }
    
    public void init(MessageDispatcher dispatcher) {
        _dispatcher=dispatcher;
    }

    public boolean isLocal() {
        return true;
    }

    public String toString() {
        return "<local>";
    }
    
    public void dispatch(ObjectMessage om, DIndexRequest request) {
        _log.info("LocalBucketDispatcher - NYI: "+request.getName());
        try {
            DIndexResponse response=null;
            if (request instanceof DIndexInsertionRequest) {
                Object oldValue=_map.put(request.getName(), om.getJMSReplyTo()); // remember location of actual session...
                _log.info("put: "+request.getName());
                response=new DIndexInsertionResponse();
            } else if (request instanceof DIndexDeletionRequest) {
                Object oldValue=_map.remove(request.getName());
                _log.info("remove: "+request.getName());
                if (oldValue==null)
                    throw new IllegalStateException();
                response=new DIndexDeletionResponse();
            } else {
                throw new UnsupportedOperationException(); // no such request - yet...
            }
            
            _dispatcher.reply(om, response);
            
        } catch (JMSException e) {
            _log.info("gor blimey!", e);
        }
    }
}
