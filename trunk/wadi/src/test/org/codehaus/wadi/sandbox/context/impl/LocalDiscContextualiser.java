/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.ContextPool;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.Promoter;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Maps id:File where file contains Context content...
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class LocalDiscContextualiser extends AbstractMappedContextualiser {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final File _dir;
	protected final StreamingStrategy _streamingStrategy;
	protected final ContextPool _pool;

	/**
	 *
	 */
	public LocalDiscContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, File dir, StreamingStrategy streamingStrategy, ContextPool pool) {
		super(next, collapser, map, evicter);
	    assert dir.exists();
	    assert dir.isDirectory();
	    assert dir.canRead();
	    assert dir.canWrite();
		_dir=dir;
		_streamingStrategy=streamingStrategy;
		_pool=pool;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres,
		FilterChain chain, String id, Promoter promoter, Sync promotionMutex) throws IOException, ServletException {
		LocalDiscMotable ldm=(LocalDiscMotable)_map.get(id);
		if (ldm!=null) {
			File file=ldm.getFile();
			Context context=null;
			try {
				context=load(file, promoter.nextContext());
			} catch (ClassNotFoundException e) {
				_log.warn("problem loading context (local disc): "+id, e);
				return false;
			}
			_log.info("promoting (from local disc): "+id);
			if (promoter.prepare(id, context)) {
				_map.remove(id);
				remove(file); // TODO - revisit
				promoter.commit(id, context);
				promotionMutex.release();
				promoter.contextualise(hreq, hres, chain, id, context);
			} else {
				promoter.rollback(id, context);
			}
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
				_log.info("demoting (to local disc): "+key);
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
				_log.info("stored (local disc): "+file);
			} catch (Exception e) {
				_log.error("store (local disc) failed: "+key, e);
			}
		}
	}

	public void evict() {
		for (Iterator i=_map.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e=(Map.Entry)i.next();
			String key=(String)e.getKey();
			LocalDiscMotable val=(LocalDiscMotable)e.getValue();
			if (_evicter.evict(key, val)) { // first test without lock - cheap
				Sync exclusive=new NullSync(); // TODO - take the promotion lock here
				try {
					if (exclusive.attempt(0) && _evicter.evict(key, val)) { // then confirm with exclusive lock
						File file=val.getFile();
						Context context=null;
						try {
							context=load(file, _pool.take());
							if (context!=null) {
								_log.info("demoting (from local disc): "+key);
								_next.demote(key, context);
								remove(file);
								i.remove();
							}
						} catch (Exception e2) {
							_log.error("could not evict file from disc: "+file, e2);
						}
						exclusive.release();
					}
				} catch (InterruptedException ie) {
					_log.warn("unexpected interruption to eviction - ignoring", ie);
				}
			}
		}
	}

	protected Context load(File file, Context context) throws ClassNotFoundException, IOException {
		assert file.exists();
		ObjectInput oi=null;
		boolean success=false;
		try {
			oi=_streamingStrategy.getInputStream(new FileInputStream(file));
			context.readContent(oi);
			_log.info("loaded (local disc): "+file);
			success=true;
		} finally {
			if (oi!=null)
				oi.close();
		}

		return success?context:null;
	}

	protected void remove(File file) {
		file.delete();
		_log.info("removed (local disc): "+file);
	}

	public boolean isLocal(){return true;}
}
