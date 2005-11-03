package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.gridstate.messages.MovePMToSM;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveIMToSM;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSMToPM;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSMToIM;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadPMToIM;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadIMToPM;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public abstract class AbstractIndirectProtocol implements Protocol {

	protected final Log _log = LogFactory.getLog(getClass());

	protected ProtocolConfig _config;

	public void init(ProtocolConfig config) {
		_config=config;
	}

	public Object onMoveBMToSM(MovePMToSM move) throws Exception {
		Object key=move.getKey();
		Object im=move.getIM();
		//Object pm=move.getPM();
		_log.info("[SM] - onMoveBMToSM@"/*+_address*/);
		_log.info("im="+im);
		Sync sync=null;
		try {
			_log.trace("onMoveBMToSM - [SM] trying for lock("+key+")...");
			sync=_config.getSMSyncs().acquire(key);
			_log.trace("onMoveBMToSM - [SM] ...lock("+key+") acquired< - "+sync);
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
			_log.trace("onMoveBMToSM - [SM] releasing lock("+key+") - "+sync);
			sync.release();
			_log.trace("onMoveBMToSM - [SM] released lock("+key+") - "+sync);
		}
	}

	public Object onReadIMToBM(ReadIMToPM read) throws Exception {
		Object key=read.getKey();
		Object im=read.getIM();
		_log.info("im="+im);
		// what if we are NOT the BM anymore ?
		// get write lock on location
		Sync sync=null;
		try {
			_log.trace("onReadIMToBM- [BM] trying for lock("+key+")...");
			sync=_config.getBMSyncs().acquire(key);
			_log.trace("onReadIMToBM- [BM] ...lock("+key+") acquired - "+sync);
			Partition partition=getPartitions()[_config.getPartitionMapper().map(key)];
			Location location=partition.getLocation(key);
			if (location==null) {
				return new ReadPMToIM();
			}
			// exchangeSendLoop GetBMToSM to SM
			Object pm=getLocalLocation();
			Object sm=location.getValue();

			MoveSMToPM response=(MoveSMToPM)syncRpc(sm, "onMoveBMToSM", new MovePMToSM(key, im, pm, null));
			// success - update location
			boolean success=response.getSuccess();
			if (success)
				location.setValue(im);

			return success?Boolean.TRUE:Boolean.FALSE;
		} finally {
			_log.trace("onReadIMToBM- [BM] releasing lock("+key+") - "+sync);
			sync.release();
			_log.trace("onReadIMToBM- [BM] released lock("+key+") - "+sync);
		}
	}

}
