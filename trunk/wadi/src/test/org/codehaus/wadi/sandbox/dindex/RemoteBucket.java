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

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RemoteBucket extends AbstractBucket {

    protected static final Log _log = LogFactory.getLog(RemoteBucket.class);

    protected Destination _location;

    public RemoteBucket(int key, Destination location) {
        super(key);
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
	  _log.info("["+_key+"] updating location from: "+_location+" to: "+location);
	  _location=location;
	}
      } else {
	if (_location.equals(location)) {
	  // no need to update
	} else {
	  _log.info("["+_key+"] updating location from: "+_location+" to: "+location);
	  _location=location;
	}
      }
    }

    public String toString() {
      return "<remote:"+(_location==null?null:_location.toString())+">";
    }

    // should deal with SessionMessages
    // what about IndexMessages (creation/destruction/migration of Sessions) ?
    public void dispatch(ObjectMessage om, DIndexRequest request) {
        // get session name from message...
        // look up location
        // if present, forward message
        // if not, reply directly
        _log.info("RemoteBucketDispatcher - NYI");
    }
}
