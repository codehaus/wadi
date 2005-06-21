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
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.BucketConfig;
import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.impl.MessageDispatcher;

public class RemoteBucket extends AbstractBucket {

    protected static final Log _log = LogFactory.getLog(RemoteBucket.class);

    protected final BucketConfig _config;
    
    protected Destination _location;

    public RemoteBucket(int key, BucketConfig config, Destination location) {
        super(key);
        _config=config;
        _location=location;
    }

    public boolean isLocal() {
        return false;
    }

    public void setLocation(Destination location) {
      if (_location==null) {
	if (location==null) {
	  // _location is already null
	} else {
	  // they cannot be equal - update
	  _log.info("["+_key+"] updating location from: "+_config.getNodeName(_location)+" to: "+_config.getNodeName(location));
	  _location=location;
	}
      } else {
	if (_location.equals(location)) {
	  // no need to update
	} else {
	  _log.info("["+_key+"] updating location from: "+_config.getNodeName(_location)+" to: "+_config.getNodeName(location));
	  _location=location;
	}
      }
    }

    public String toString() {
      return "<remote:"+(_location==null?null:_config.getNodeName(_location))+">";
    }

    public void dispatch(ObjectMessage om, DIndexRequest request) {
        try {
            _log.info("dispatch: "+request.getClass().getName()+" - "+request.getName()+" - "+_config.getNodeName(om.getJMSReplyTo())+" -> "+_config.getNodeName(_location));
//            if (request instanceof DIndexForwardRequest) {
//                _log.info("unwrap forwarded request: ");
//                om.setObject(((DIndexForwardRequest)request).getRequest());
//            }
            _config.getMessageDispatcher().forward(om, _location);
        } catch (JMSException e) {
            _log.warn("could not forward message", e);
        }
    }
}
