/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.Promoter;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * Maps id:File where file contains Context content...
 */
public class LocalDiscContextualiser extends AbstractMappedContextualiser {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final File _dir;
	protected final StreamingStrategy _streamingStrategy;
	
	/**
	 * 
	 */
	public LocalDiscContextualiser(Contextualiser next, Map map, Evicter evicter, File dir, StreamingStrategy streamingStrategy) {
		super(next, map, evicter);
	    assert dir.exists();
	    assert dir.isDirectory();
	    assert dir.canRead();
	    assert dir.canWrite();
		_dir=dir;
		_streamingStrategy=streamingStrategy;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualiseLocally(ServletRequest req, ServletResponse res,
		FilterChain chain, String id, Promoter promoter, Sync promotionMutex) throws IOException, ServletException {
		File file=((LocalDiscMotable)_map.get(id)).getFile();
		if (file.exists()) {
			Context context=promoter.nextContext();
			ObjectInput oi=_streamingStrategy.getInputStream(new FileInputStream(file));
			try {
				context.readContent(oi);
			} catch (ClassNotFoundException e) {
				throw new ServletException("problem loading context (local disc): "+id, e);
			}
			oi.close();
			_log.info("loaded (local disc): "+file);
			_log.info("promoting: "+id);
			promoter.promoteAndContextualise(req, res, chain, id, context, promotionMutex); // inject result into our caller - now available to new threads
			file.delete(); // perhaps this should be wrapped up in a callback object and passed up to the promoter with the promotionMutex - otherwise file is not removed until after request has run...
			// this is important since the session may be written out again BEFORE we actually get around to deleting the file !
			_map.remove(id);
			_log.info("removed (local disc): "+file);
			return true;
		} else {
			return false;
		}
	}
	
	public Promoter getPromoter(Promoter promoter){return promoter;} // just pass contexts straight through...
	
	static public class LocalDiscMotable implements Motable {
		protected final long _expiryTime;
		protected final File _file;
		
		public LocalDiscMotable(long expiryTime, File file) {
			_expiryTime=expiryTime;
			_file=file;
		}

		public long getExpiryTime() {return _expiryTime;}
		public File getFile() {return _file;}
	}
	
	public void demote(String key, Motable val) {
		if (_evicter.evict(key, val)) {
			_next.demote(key, val);
		} else {
			try {
				SerializableContent sc=(SerializableContent)val;
				File file=new File(_dir, key.toString()+"."+_streamingStrategy.getSuffix());
				ObjectOutput oos=_streamingStrategy.getOutputStream(new FileOutputStream(file));
				sc.writeContent(oos);
				oos.flush();
				oos.close();

				long expiryTime=val.getExpiryTime();
				// do we need to worry about this...
				//	file.setLastModified(expiryTime);
				_map.put(key, new LocalDiscMotable(expiryTime, file));
				_log.info("stored (local disc): "+key+" : "+val);
			} catch (Exception e) {
				_log.error("store (local disc) failed: "+key, e);
			}
		}
	}
}
