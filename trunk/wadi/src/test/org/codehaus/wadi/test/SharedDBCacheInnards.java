
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheEntry;
import javax.cache.CacheException;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.StreamingStrategy;

//----------------------------------------

// first, and very rudimentary, shot at a DB-backed JCache - lots of
// work to do here...

// classloading ?

// shouldn't be caching keys...

// refactor and merge as much as possible with LocalDiscCacheInnards
//----------------------------------------

class
  SharedDBCacheInnards
  extends AbstractCacheInnards
{
  protected final Log _log=LogFactory.getLog(getClass());

  protected final DataSource              _ds;
  protected final String                  _table;

  //----------------------------------------

  public
    SharedDBCacheInnards(DataSource ds, String table, SerializableContentPool valuePool, StreamingStrategy streamingStrategy, Cache cache, EvictionPolicy evictionPolicy)
  {
    super(valuePool, streamingStrategy, cache, evictionPolicy);
    assert ds!=null;
    assert table!=null;

    _ds                =ds;
    _table             =table;
    _log.info("created: "+this);
  }

  public String
    toString()
  {
    return "<"+getClass().getName()+":"+_ds+", "+_table+", "+_valuePool+", "+_cache+">";
  }

  // need to think about whether to catch exception here or in code above...
  protected Object
    loadValue(Object key)
  {
    Object value=null;
    try
    {
      Connection c=_ds.getConnection();
      Statement s=c.createStatement();
      ResultSet rs=s.executeQuery("SELECT MyValue FROM "+_table+" WHERE MyKey='"+key+"'");
      if (rs.next())
      {
	SerializableContent sc=(SerializableContent)rs.getObject(1);
	value=sc;
	_log.info("loaded (database): "+key+" : "+value);
      }

      s.close();
      c.close();
    }
    catch (Exception e)
    {
      _log.error("loading (database) failed: "+key, e);
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

      Connection c=_ds.getConnection();
      PreparedStatement ps=c.prepareStatement("UPDATE "+_table+" SET MyValue=? WHERE MyKey='"+key+"'");
      ps.setObject(1, value);
      ps.executeUpdate();
      ps.close();
      c.close();

      // do we need to worry about ttl ?
      //	long willTimeOutAt=impl.getLastAccessedTime()+(impl.getMaxInactiveInterval()*1000);
      //	file.setLastModified(willTimeOutAt);

      _log.info("stored (database): "+key+" : "+value);
      success=true;
    }
    catch (Exception e)
    {
      _log.error("eviction (database) failed: "+key, e);
    }

    return success;
  }

  protected boolean
    removeValue(Object key)
  {
    boolean success=false;

    try
    {
      Connection c=_ds.getConnection();
      Statement s=c.createStatement();
      int r=s.executeUpdate("DELETE FROM "+_table+" WHERE MyKey='"+key+"'");
      s.close();
      c.close();

      _log.info("removed (database): "+key);
      success=true;
    }
    catch (Exception e)
    {
      _log.error("removal (database) failed: "+key, e);
    }

    return success;
  }
}
