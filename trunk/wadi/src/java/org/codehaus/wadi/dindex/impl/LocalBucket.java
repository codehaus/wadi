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
import org.codehaus.wadi.impl.MessageDispatcher;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

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
        return "<local>";
    }
    
    public void dispatch(ObjectMessage om, DIndexRequest request) {
        try {
            DIndexResponse response=null;
            if (request instanceof DIndexInsertionRequest) {
                Destination location=om.getJMSReplyTo();
                Object oldValue=_map.put(request.getName(), location); // remember location of actual session...
                _log.info("put "+request.getName()+" : "+_config.getNodeName(location));
                response=new DIndexInsertionResponse();
            } else if (request instanceof DIndexDeletionRequest) {
                Object oldValue=_map.remove(request.getName());
                _log.info("remove "+request.getName()+" : "+_config.getNodeName((Destination)oldValue));
                if (oldValue==null)
                    throw new IllegalStateException();
                response=new DIndexDeletionResponse();
            } else {
                String name=request.getName();
                Destination location=(Destination)_map.get(name);
                _log.info("forwarding: "+request.getName()+" to: "+_config.getNodeName(location));
                try {
                    _config.getMessageDispatcher().forward(om, location);
                } catch (JMSException e) {
                    _log.warn("could not forward message", e);
                }
                //throw new UnsupportedOperationException(); // no such request - yet...
            }
            // we can optimise local-local send here - TODO
            _config.getMessageDispatcher().reply(om, response);
            
        } catch (JMSException e) {
            _log.info("gor blimey!", e);
        }
    }
}
