
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

package org.codehaus.wadi.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.cache.Cache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.StreamingStrategy;

//----------------------------------------

// assumption - we are the only people reading and writing to this
// directory... - should be dynamically created to ensure this...

// if we know the cache is homogeneous (only stores one type), we do
// not need to write type information into every evicted file... -
// hence SerializableContent...

//----------------------------------------

public class
  LocalDiscCacheInnards
  extends AbstractCacheInnards
{
  protected final Log _log=LogFactory.getLog(getClass());

  protected final File                    _dir;

  //----------------------------------------

  public
    LocalDiscCacheInnards(File dir, SerializableContentPool valuePool, StreamingStrategy streamingStrategy, Cache cache, EvictionPolicy evictionPolicy)
  {
    super(valuePool, streamingStrategy, cache, evictionPolicy);
    assert dir.exists();
    assert dir.isDirectory();
    assert dir.canRead();
    assert dir.canWrite();

    _dir               =dir;
    _log.info("created: "+this);
  }

  public String
    toString()
  {
    return "<"+getClass().getName()+":"+_dir+", "+_valuePool+", "+_cache+">";
  }

  // need to think about whether to catch exception here or in code above...
  protected Object
    loadValue(Object key)
  {
    Object value=null;

    File file=new File(_dir, key.toString()+"."+_streamingStrategy.getSuffix());
    if (file.exists())
    {
      try
      {
	SerializableContent sc=_valuePool.take();
	ObjectInput oi=_streamingStrategy.getInputStream(new FileInputStream(file));
	sc.readContent(oi);
	oi.close();

	value=sc;
	_log.info("invicted (local disc): "+key+" : "+value);
      }
      catch (Exception e)
      {
	_log.error("inviction (local disc) failed: "+key, e);
      }
    }

    return value;
  }

  protected boolean
    storeValue(Object key, Object value)
  {
    boolean success=false;

    try
    {
      SerializableContent sc=(SerializableContent)value;
      File file=new File(_dir, key.toString()+"."+_streamingStrategy.getSuffix());
      ObjectOutput oos=_streamingStrategy.getOutputStream(new FileOutputStream(file));
      sc.writeContent(oos);
      oos.flush();
      oos.close();

      // do we need to worry about ttl ?
      //	long willTimeOutAt=impl.getLastAccessedTime()+(impl.getMaxInactiveInterval()*1000);
      //	file.setLastModified(willTimeOutAt);

      _log.info("evicted (local disc): "+key+" : "+value);
      success=true;
    }
    catch (Exception e)
    {
      _log.error("eviction (local disc) failed: "+key, e);
    }

    return success;
  }

  protected boolean
  	addValue(Object key, Object value)
  {
  	return storeValue(key, value);
  }
  
  protected boolean
    removeValue(Object key)
  {
    boolean success=false;

    try
    {
      File file=new File(_dir, key.toString()+"."+_streamingStrategy.getSuffix());
      file.delete();
      _log.info("removed (local disc): "+key);
      success=true;
    }
    catch (Exception e)
    {
      _log.error("removal (local disc) failed: "+key, e);
    }

    return success;
  }
}
