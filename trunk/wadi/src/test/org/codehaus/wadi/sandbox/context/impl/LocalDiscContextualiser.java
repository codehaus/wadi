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
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Collapser;
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
	protected static final Log _log = LogFactory.getLog(LocalDiscContextualiser.class);
	
	protected final StreamingStrategy _streamer;
	protected final File _dir;

	public LocalDiscContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, StreamingStrategy streamer, File dir) {
		super(next, collapser, map, evicter);
		_streamer=streamer;
	    assert dir.exists();
	    assert dir.isDirectory();
	    assert dir.canRead();
	    assert dir.canWrite();
		_dir=dir;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres,
		FilterChain chain, String id, Promoter promoter, Sync promotionLock) throws IOException, ServletException {
		LocalDiscMotable ldm=(LocalDiscMotable)_map.get(id);
		if (ldm!=null) {
			Motable motable=promoter.nextMotable();
			try {
				motable.setBytes(ldm.getBytes());
			} catch (Exception e) {
				_log.warn("problem loading context (local disc): "+id, e);
				return false;
			}
			_log.info("promoting (from local disc): "+id);
			if (promoter.prepare(id, motable)) {
				_map.remove(id);
				remove(ldm.getFile()); // TODO - revisit
				promoter.commit(id, motable);
				promotionLock.release();
				promoter.contextualise(hreq, hres, chain, id, motable);
			} else {
				promoter.rollback(id, motable);
			}
			return true;
		} else {
			return false;
		}
	}

	public Promoter getPromoter(Promoter promoter){return promoter;} // just pass contexts straight through...

	public static class LocalDiscMotable implements Motable {
		protected final long _expiryTime;
		protected final File _file;
		
		public LocalDiscMotable(long expiryTime, File file) {
			_expiryTime=expiryTime;
			_file=file;
		}
		public File getFile() {return _file;}
		
		// Evictable
		public long getExpiryTime() {return _expiryTime;}
		
		// Motable
		public byte[] getBytes() throws IOException {return load(_file);}
		public void setBytes(byte[] bytes) throws IOException {store(_file, bytes);}
	}

	public void demote(String key, Motable val) {
		if (_evicter.evict(key, val)) {
			_next.demote(key, val);
		} else {
			try {
				_log.info("demoting (to local disc): "+key);
				File file=new File(_dir, key.toString()+"."+_streamer.getSuffix());
				long expiryTime=val.getExpiryTime();
				Motable motable=new LocalDiscMotable(expiryTime, file);
				motable.setBytes(val.getBytes());
				_map.put(key, motable);
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
						try {
							_log.info("demoting (from local disc): "+key);
							_next.demote(key, val);
							i.remove();
							remove(val.getFile());
						} catch (Exception e2) {
							_log.error("could not evict file from disc: "+val.getFile(), e2);
						}
						exclusive.release();
					}
				} catch (InterruptedException ie) {
					_log.warn("unexpected interruption to eviction - ignoring", ie);
				}
			}
		}
	}

	protected static byte[] load(File file) throws IOException {
		int length=(int)file.length(); // length must be OK, or file would not exist
		FileInputStream fis=null;
		try {
			fis=new FileInputStream(file);
			byte[] buffer=new byte[length];
			fis.read(buffer, 0 ,length);
			_log.info("loaded (local disc): "+file);
			return buffer;
		} catch (IOException e) {
			_log.warn("load (local disc) failed: "+file, e);
			throw e;
		}
		finally {
			if (fis!=null)
				fis.close();
		}
	}
	
	protected static void store(File file, byte[] bytes) throws IOException {
		OutputStream os=null;
		try {
			os=new FileOutputStream(file);
			os.write(bytes);
			os.flush();
			_log.info("stored (local disc): "+file);
		} catch (IOException e) {
			_log.warn("store (local disc) failed: "+file, e);
			throw e;
		} finally {
			if (os!=null)
				os.close();			
		}
	}
	
	protected void remove(File file) {
		file.delete();
		_log.info("removed (local disc): "+file);
	}

	public boolean isLocal(){return true;}
}
