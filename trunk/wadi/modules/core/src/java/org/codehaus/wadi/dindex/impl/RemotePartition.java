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
import org.codehaus.wadi.dindex.PartitionConfig;
import org.codehaus.wadi.dindex.DIndexRequest;

public class RemotePartition extends AbstractPartition {
	
	protected static final Log _log = LogFactory.getLog(RemotePartition.class);
	
	protected final PartitionConfig _config;
	
	protected Destination _location;
	
	public RemotePartition(int key, PartitionConfig config, Destination location) {
		super(key);
		_config=config;
		_location=location;
	}
	
	public boolean isLocal() {
		return false;
	}
	
	public Destination getDestination() {
		return _location;
	}
	
	public void setLocation(Destination location) {
		if (_location==null) {
			if (location==null) {
				// _location is already null
			} else {
				// they cannot be equal - update
				if (_log.isTraceEnabled()) _log.trace("[" + _key + "] updating location from: " + _config.getNodeName(_location) + " to: " + _config.getNodeName(location));
				_location=location;
			}
		} else {
			if (_location.equals(location)) {
				// no need to update
			} else {
				if (_log.isTraceEnabled()) _log.trace("[" + _key + "] updating location from: " + _config.getNodeName(_location) + " to: " + _config.getNodeName(location));
				_location=location;
			}
		}
	}
	
	public String toString() {
		return "<remote:"+(_location==null?null:_config.getNodeName(_location))+">";
	}
	
	public void dispatch(ObjectMessage om, DIndexRequest request) {
		if (_log.isTraceEnabled()) _log.trace("indirecting: " + request + " via " + _config.getNodeName(_location));
		if (!_config.getDispatcher().forward(om, _location))
			_log.warn("could not forward message");
	}
	
}
