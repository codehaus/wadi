/*
 * Created on Feb 23, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.Location;

import EDU.oswego.cs.dl.util.concurrent.Sync;

// how are we going to test this ?

/**
 * @author jules
 * 
 * Either proxy req/res pair to a remote webcontainer, or migrate required
 * Context here so that the req/res may be contextualised locally...
 * 
 * We use the same HttpConnection over which the req/res pair may be proxied for
 * the synchronous migration of the session (This will require extra support
 * installed in the WADI Filter).
 */
public class PiggyBackHttpLocation implements Location {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final int _threshold;
	/**
	 *  
	 */
	public PiggyBackHttpLocation(int threshold) {
		super();
		_threshold=threshold;
	}

	/*
	 * The idea is to count consecutive proxies from the same node. If another
	 * node intervenes, the count is reset. If the count is reached, instead of
	 * subsequent proxying occurring, the context is migrated to the successful
	 * applicant. This should prevent Session ping-pong migration when the
	 * load-balancer gets confused. Migration should finally occur only when the
	 * load-balancer becomes consistant in its choice.
	 * 
	 * @see org.codehaus.wadi.sandbox.context.Location#proxy(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse,
	 *      EDU.oswego.cs.dl.util.concurrent.Sync)
	 */
	
	protected Object _countLock=new Object();
	protected int _activeCount;
	protected int _successCount;
	protected int _lastSequence;
	protected int _threadsIn;
	protected int _threadsOut;
	
	public String toString() {
		synchronized (_countLock) {
		return "active:"+_activeCount+", success:"+_successCount+", sequence:"+_lastSequence+", threadsIn:"+_threadsIn+", threadsOut:"+_threadsOut;
		}
	}
	
	public Context proxy(ServletRequest req, ServletResponse res, String id, Sync promotionLock) {
		// Either we pull the context across the connection and return it...

		synchronized (_countLock) {
			// if there is room for another thread to run, let one in...
			++_threadsIn;
			if (++_activeCount<_threshold) {
				promotionLock.release();
			}
			_log.info("--> "+this);
		}


		// do some work...
		Result result=doit(req, res, id);
		int successes=result._successes;
		int sequence=result._sequence;
		
		synchronized (_countLock) {
			_activeCount--;
			// if this thread is bringing fresh data, update our model
			if (sequence>_lastSequence) {
				_lastSequence=sequence;
				_successCount=successes;
			}
			
			_log.info("<-- "+this);

			// run round again...
			if (_successCount>=_threshold) {
				_log.info("made it ! : "+_successCount);
				_successCount=0;
				_serverSuccesses=0;
			}
			// when we actually hit a success, we need to wait for all
			// threads to return, then try to do the migration, then if
			// successful promote, otherwise open the gates again...
			

			// if there is room for another thread, bring it on...
			// need to say - AND mutex has outstanding locks....
			
			// threadsin/Out doesn't work...
			
			++_threadsOut;
			if ((_activeCount+_successCount)<_threshold && _threadsOut<=_threadsIn)
				promotionLock.release();
			
			// will releasing more times than necessary break the Mutex - or might we get away with it ?
	}
		
		// or proxy across to it and return null...
		// do the HttpProxying here....
		
		// use Greg's proxy code here....
	
		return null;
	}

	protected int _sequence=0;
	protected Object _sequenceLock=new Object();
	
	class Result {
		final int _successes;
		final int _sequence;
		
		Result(int successes, int sequence) {
			_successes=successes;
			_sequence=sequence;
		}
	}
	
	//--------------------------------------------------------------
	
	protected int _serverSuccesses=0;
	
	public Result doit(ServletRequest req, ServletResponse res, String id) {
		// allocate sequence id
		int copy;
		synchronized (_sequenceLock) { copy=++_sequence; }
		
		// spend some time working
		try {
			_log.info("processing: "+id);
			Thread.sleep(4000);
			_log.info("finished: "+id);
		} catch (InterruptedException ignore) {
			// ignore
		}
		
		// success or failure ?
		_serverSuccesses++;
		
		return new Result(_serverSuccesses, copy);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.codehaus.wadi.sandbox.context.Motable#getExpiryTime()
	 */
	public long getExpiryTime() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	// All requests are passed through this filter - if they are proxies. filter will do the right thing...
	
//	protected String getClient(ServletRequest req){return "NYI";}
//	
//	protected String _lastClient;
//	protected int _lastClientCount;
//	
//	public void filter(ServletRequest req, ServletResponse res) {
//		// talk to Greg about exactly how we implement this...
//		String client=getClient(req);
//		int countDown=_threshold;
//		synchronized (this) { // synched on session id
//			if (_lastClient.equals(client)) {
//				_lastClientCount++;
//			} else {
//				_lastClient=client;
//				_lastClientCount=0;
//			}
//			countDown-=_lastClientCount;
//		}
//		
//		// on way out, write countDown value as Header into response...
//	}
}