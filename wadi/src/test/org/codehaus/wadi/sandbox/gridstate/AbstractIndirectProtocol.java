package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveBOToSO;
import org.codehaus.wadi.sandbox.gridstate.messages.MovePOToSO;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSOToBO;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadBOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadPOToBO;
import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public abstract class AbstractIndirectProtocol implements Protocol {

	protected final Log _log = LogFactory.getLog(getClass());

	protected ProtocolConfig _config;

	public void init(ProtocolConfig config) {
		_config=config;
	}

	public Object onMoveBOToSO(MoveBOToSO move) throws Exception {
		Object key=move.getKey();
		Object po=move.getPO();
		Object bo=move.getBO();
		_log.info("[SO] - onMoveBOToSO@"/*+_address*/);
		_log.info("po="+po);
		Sync sync=null;
		try {
			_log.info("onMoveBOToSO - [SO] trying for lock("+key+")...");
			sync=_config.getSOSyncs().acquire(key);
			_log.info("onMoveBOToSO - [SO] ...lock("+key+") acquired< - "+sync);
			// send GetSOToPO to PO
			Object value;
			Map map=_config.getMap();
			synchronized (map) {
				value=map.get(key);
			}
			_log.info("[SO] sending "+key+"="+value+" -> PO...");
			MovePOToSO response=(MovePOToSO)syncRpc(po, "onMoveSOToPO", new MoveSOToPO(key, value));
			_log.info("[SO] ...response received <- PO");
			boolean success=response.getSuccess();
			if (success) {
				synchronized (map) {
					map.remove(key);
					return new MoveSOToBO();
				}
			}
			return new MoveSOToBO(success);
		} finally {
			_log.info("onMoveBOToSO - [SO] releasing lock("+key+") - "+sync);
			sync.release();
		}
	}

	public Object onReadPOToBO(ReadPOToBO read) throws Exception {
		Object key=read.getKey();
		Object po=read.getPO();
		_log.info("po="+po);
		// what if we are NOT the BO anymore ?
		// get write lock on location
		Sync sync=null;
		try {
			_log.info("onReadPOToBO- [BO] trying for lock("+key+")...");
			sync=_config.getBOSyncs().acquire(key);
			_log.info("onReadPOToBO- [BO] ...lock("+key+") acquired - "+sync);
			Bucket bucket=getBuckets()[_config.getBucketMapper().map(key)];
			Location location=bucket.getLocation(key);
			if (location==null) {
				return new ReadBOToPO();
			}
			// exchangeSendLoop GetBOToSO to SO
			Object bo=getLocalLocation();
			Object so=location.getValue();
			
			MoveSOToBO response=(MoveSOToBO)syncRpc(so, "onMoveBOToSO", new MoveBOToSO(key, po, bo, null));
			// success - update location
			boolean success=response.getSuccess();
			if (success)
				location.setValue(po);
			
			return success?Boolean.TRUE:Boolean.FALSE; 
		} finally {
			_log.info("onReadPOToBO- [BO] releasing lock("+key+") - "+sync);
			sync.release();
		}	
	}

}
