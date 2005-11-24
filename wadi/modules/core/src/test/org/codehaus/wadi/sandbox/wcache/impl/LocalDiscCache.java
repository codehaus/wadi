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
/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.wcache.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutput;

import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.sandbox.wcache.Cache;
import org.codehaus.wadi.sandbox.wcache.RequestProcessor;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * @author jules
 *
 * Promotion from this cache will result in the loading of content from local disc
 * Demotion to this cache will result in the storing of content onto local disc.
 * Assumptions are made about the exclusive ownership of the directory and files used.
 * Content keys will be cached in memory, values on disc.
 *
 * This tier is intended as a disc spool for large amounts of frequently used content.
 *
 * This could return lazy references to on disc objects... consider...
 */
public class LocalDiscCache extends AbstractMappedCache {

	protected File _dir;
	protected Streamer _streamingStrategy;

	public LocalDiscCache(File dir, Streamer streamingStrategy, Evicter evicter, Cache subcache) {
		super(new ConcurrentHashMap(), evicter, subcache);
	    assert dir.exists();
	    assert dir.isDirectory();
	    assert dir.canRead();
	    assert dir.canWrite();
	    _dir=dir;
	    _streamingStrategy=streamingStrategy;
	}

	public RequestProcessor put(String key, RequestProcessor val) {
		try
		{
			SerializableContent sc=val;
			File file=new File(_dir, key.toString()+"."+_streamingStrategy.getSuffix());
			ObjectOutput oos=_streamingStrategy.getOutputStream(new FileOutputStream(file));
			sc.writeContent(oos);
			oos.flush();
			oos.close();

			// do we need to worry about ttl ?
			//	long willTimeOutAt=impl.getLastAccessedTime()+(impl.getMaxInactiveInterval()*1000);
			//	file.setLastModified(willTimeOutAt);

            if (_log.isInfoEnabled()) _log.info("stored (local disc): " + key + " : " + val);
		}
		catch (Exception e)
		{
            if (_log.isErrorEnabled()) _log.error("store (local disc) failed: " + key, e);
		}

		return (RequestProcessor)_map.put(key, val);
	}

	public RequestProcessor peek(String key) {
//	    Object value=null;
//
//	    File file=new File(_dir, key.toString()+"."+_streamingStrategy.getSuffix());
//	    if (file.exists())
//	    {
//	      try
//	      {
//		// SerializableContent sc=_valuePool.take();
//	      	SerializableContent sc=new RequestProcessor(); // what are we going to do here - we need to decide how type chages as it is pro/demoted up stack...
//		ObjectInput oi=_streamingStrategy.getInputStream(new FileInputStream(file));
//		sc.readContent(oi);
//		oi.close();
//
//		value=sc;
//		_log.info("loaded (local disc): "+key+" : "+value);
//	      }
//	      catch (Exception e)
//	      {
//		_log.error("load (local disc) failed: "+key, e);
//	      }
//	    }
//
//	    //return value;
		return (RequestProcessor)_map.get(key);
		}

	public RequestProcessor remove(String key) { // Aaargh - we don't want to return a value here - too costly... or can we get away with just two methods in a store ?
//		   boolean success=false;
//
//		    try
//		    {
//		      File file=new File(_dir, key.toString()+"."+_streamingStrategy.getSuffix());
//		      file.delete();
//		      _log.info("removed (local disc): "+key);
//		      success=true;
//		    }
//		    catch (Exception e)
//		    {
//		      _log.error("removal (local disc) failed: "+key, e);
//		    }
//
//		    return success;
		    return (RequestProcessor)_map.remove(key);
		}

	public boolean isOffNode() {return false;}
}
