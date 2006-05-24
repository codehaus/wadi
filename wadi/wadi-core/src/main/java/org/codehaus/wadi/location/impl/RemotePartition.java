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
package org.codehaus.wadi.location.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.location.DIndexRequest;
import org.codehaus.wadi.location.PartitionConfig;
import org.codehaus.wadi.location.newmessages.DeleteIMToPM;
import org.codehaus.wadi.location.newmessages.EvacuateIMToPM;
import org.codehaus.wadi.location.newmessages.InsertIMToPM;
import org.codehaus.wadi.location.newmessages.MoveIMToPM;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class RemotePartition extends AbstractPartition {

	protected transient Log _log;

	protected final PartitionConfig _config;

	protected Address _location;

	public RemotePartition(int key, PartitionConfig config, Address address) {
		super(key);
		_config=config;
		_location=address;
		_log=LogFactory.getLog(getClass().getName()+"#"+_key+"@"+_config.getLocalPeerName());
	}

	public boolean isLocal() {
		return false;
	}

	public Address getAddress() {
		return _location;
	}

	public void setAddress(Address address) {
		if (_location==null) {
			if (address==null) {
				// _location is already null
			} else {
				// they cannot be equal - update
				if (_log.isTraceEnabled()) _log.trace("[" + _key + "] updating location from: " + _config.getPeerName(_location) + " to: " + _config.getPeerName(address));
				_location=address;
			}
		} else {
			if (_location.equals(address)) {
				// no need to update
			} else {
				if (_log.isTraceEnabled()) _log.trace("[" + _key + "] updating location from: " + _config.getPeerName(_location) + " to: " + _config.getPeerName(address));
				_location=address;
			}
		}
	}

	public String toString() {
		return "<"+getClass()+":"+_key+"@"+_config.getLocalPeerName()+"->"+_config.getPeerName(_location)+">";
	}

	public void onMessage(Message message, InsertIMToPM request) {
		if (_log.isTraceEnabled()) _log.trace("#"+_key+" : forwarding: " + request + " from "+_config.getLocalPeerName()+" to " + _config.getPeerName(_location));

        forward(message);
	}

	public void onMessage(Message message, DeleteIMToPM request) {
		if (_log.isTraceEnabled()) _log.trace("indirecting: " + request + " via " + _config.getPeerName(_location));

        forward(message);
	}

	public void onMessage(Message message, EvacuateIMToPM request) {
		if (_log.isTraceEnabled()) _log.trace("indirecting: " + request + " via " + _config.getPeerName(_location));

        forward(message);
	}

	public void onMessage(Message message, MoveIMToPM request) {
		if (_log.isWarnEnabled()) _log.warn(_config.getLocalPeerName()+": not Master of Partition["+_key+"] - forwarding message to "+_config.getPeerName(_location));
        
        forward(message);
	}

	public Message exchange(DIndexRequest request, long timeout) throws Exception {
		Dispatcher dispatcher=_config.getDispatcher();
		Address target=_location;
		if (_log.isTraceEnabled()) _log.trace("exchanging message ("+request+") with node: "+_config.getPeerName(target)+" on "+Thread.currentThread().getName());
		return dispatcher.exchangeSend(target, request, timeout);
	}
    
    private void forward(Message message) {
        try {
            _config.getDispatcher().forward(message, _location);
        } catch (MessageExchangeException e) {
            _log.warn("could not forward message", e);
        }
    }
}
