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
package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Dispatcher;
import org.codehaus.wadi.sandbox.gridstate.messages.MovePMToSM;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveIMToSM;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSMToPM;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSMToIM;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadPMToIM;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadIMToPM;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public abstract class AbstractIndirectProtocol implements Protocol, PartitionConfig {

	protected final Log _log = LogFactory.getLog(getClass());

	protected final String _clusterName = "ORG.CODEHAUS.WADI.TEST";
	protected final String _nodeName;
	protected final PartitionManager _partitionManager;
	protected final long _timeout;

	protected Dispatcher _dispatcher; // should be final

    public AbstractIndirectProtocol(String nodeName, PartitionManager manager, PartitionMapper mapper, long timeout, Dispatcher dispatcher) throws Exception {
    	_nodeName=nodeName;
    	(_partitionManager=manager).init(this);
    	_timeout=timeout;
    	_dispatcher=dispatcher;

    }

	protected ProtocolConfig _config;

	public void init(ProtocolConfig config) {
		_config=config;
	}

	public Object onMovePMToSM(MovePMToSM move) throws Exception {
		Object key=move.getKey();
		Object im=move.getIM();
		//Object pm=move.getPM();
		_log.info("[SM] - onMovePMToSM@"/*+_address*/);
		_log.info("im="+im);
		Sync sync=null;
		try {
			_log.trace("onMovePMToSM - [SM] trying for lock("+key+")...");
			sync=_config.getSMSyncs().acquire(key);
			_log.trace("onMovePMToSM - [SM] ...lock("+key+") acquired< - "+sync);
			// send GetSMToIM to IM
			Object value;
			Map map=_config.getMap();
			synchronized (map) {
				value=map.get(key);
			}
			_log.info("[SM] sending "+key+"="+value+" -> IM...");
			MoveIMToSM response=(MoveIMToSM)syncRpc(im, "onMoveSMToIM", new MoveSMToIM(key, value));
			_log.info("[SM] ...response received <- IM");
			boolean success=response.getSuccess();
			if (success) {
				synchronized (map) {
					map.remove(key);
					return new MoveSMToPM();
				}
			}
			return new MoveSMToPM(success);
		} finally {
			_log.trace("onMovePMToSM - [SM] releasing lock("+key+") - "+sync);
			sync.release();
			_log.trace("onMovePMToSM - [SM] released lock("+key+") - "+sync);
		}
	}

	public Object onReadIMToPM(ReadIMToPM read) throws Exception {
		Object key=read.getKey();
		Object im=read.getIM();
		_log.info("im="+im);
		// what if we are NOT the PM anymore ?
		// get write lock on location
		Sync sync=null;
		try {
			_log.trace("onReadIMToPM- [PM] trying for lock("+key+")...");
			sync=_config.getPMSyncs().acquire(key);
			_log.trace("onReadIMToPM- [PM] ...lock("+key+") acquired - "+sync);
			Partition partition=getPartitions()[_config.getPartitionMapper().map(key)];
			Location location=partition.getLocation(key);
			if (location==null) {
				return new ReadPMToIM();
			}
			// exchangeSendLoop GetPMToSM to SM
			Object pm=getLocalLocation();
			Object sm=location.getValue();

			MoveSMToPM response=(MoveSMToPM)syncRpc(sm, "onMovePMToSM", new MovePMToSM(key, im, pm, null));
			// success - update location
			boolean success=response.getSuccess();
			if (success)
				location.setValue(im);

			return success?Boolean.TRUE:Boolean.FALSE;
		} finally {
			_log.trace("onReadIMToPM- [PM] releasing lock("+key+") - "+sync);
			sync.release();
			_log.trace("onReadIMToPM- [PM] released lock("+key+") - "+sync);
		}
	}

	public Partition[] getPartitions() {
		return _partitionManager.getPartitions();
	}

}
