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

 package org.codehaus.wadi.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.server.UID;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.shared.HttpSessionImpl;
import org.codehaus.wadi.shared.PassivationStrategy;

// TODO - will activate() use the right ClassLoader ?

// TODO - the UID I am using here is probably not ideal - we need a
// better way to avoid race conditions between two nodes looking at
// the same filespace...

// TODO - what should we do with session files that will no longer
// load (e.g. due to incompatible classes)?

// TODO - passivation should be reworked to remove the tmp file if
// anything goes wrong ?

// TODO - broken files should not be removed, but moved to on side...

// TODO - do we need to worry about system limits on file size/numbers ?

// TODO - we could isolate and partition this data onto many different NFS
// servers for resilience and scaling etc...

// TODO - only one node should be running the GC ... consider...

/**
 * A MigrationPolicy that uses a local or shared file system on which
 * to passivate evicted sessions. WARNING - the session file's
 * LastModified time is used to signal when it should be timed out, so
 * altering this may have unforseen effects.
 *
 * @author <a href="mailto:jules@mortbay.com">Jules Gosnell</a>
 * @version 1.0
 */
public class
  OldFilePassivationStrategy
  implements PassivationStrategy
{
  protected final Log    _log    =LogFactory.getLog(getClass());
  protected final String _suffix =".ser";
  protected final String _uid    =new UID().toString();
  protected final File   _dir;

  public
    OldFilePassivationStrategy(String dir)
    {
      this(new File(dir));
    }

  public
    OldFilePassivationStrategy(File dir)
    {
      _dir=dir;

      if (!_dir.exists() && !_dir.mkdirs())
	throw new IllegalArgumentException("could not create dir: "+_dir);

      if (!(_dir.exists() &&
	    _dir.isDirectory() &&
	    _dir.canRead() &&
	    _dir.canWrite()))
	throw new IllegalArgumentException("bad dir (is it rw?): "+_dir);
    }

  public boolean
    passivate(HttpSessionImpl impl)
    {
      String id=impl.getId();
      File file=null;
      try
      {
	file=new File(_dir, id+"-passivating-"+_uid+_suffix);
	FileOutputStream fos=new FileOutputStream(file);
	File target=new File(_dir, id+_suffix);
	_log.trace(target.toString()+": passivating : "+impl);
	ObjectOutputStream oos=new ObjectOutputStream(fos);
	oos.writeObject(impl);
	oos.flush();
	oos.close();
	if (target.exists())
	{
	  _log.warn(target.toString()+": could not passivate - id already in use");
	  file.delete();
	  return false;
	}
	else
	{
	  long willTimeOutAt=impl.getLastAccessedTime()+(impl.getMaxInactiveInterval()*1000);
	  file.setLastModified(willTimeOutAt);
	  file.renameTo(target);
	  return true;
	}
      }
      catch (Exception e)
      {
	_log.warn(file.toString()+": problem storing passivated session", e);
	return false;
      }
    }

  public HttpSessionImpl
    activate(String id)
  {
    HttpSessionImpl impl=null;
    File file=null;
    try
    {
      file=new File(_dir, id+_suffix);
      // quick exit - saves expensive allocation and stack unwinding
      // if file not present..
      if (!file.exists())
	return impl;
      else
      {
	boolean renamed=false;
	File target=null;
	try
	{
	  target=new File(_dir, id+"_activating-"+_uid+_suffix);
	  renamed=file.renameTo(target);
	  FileInputStream fis=new FileInputStream(target);
	  ObjectInputStream ois=new ObjectInputStream(fis);
	  impl=(HttpSessionImpl)ois.readObject();
	  ois.close();
	  _log.trace(file.toString()+": activating : "+impl);
	  return impl;
	}
	finally
	{
	  if (renamed)
	    target.delete();
	}
      }
    }
    catch (FileNotFoundException e)
    {
      // someone else grabbed it whilst we were thinking about it -
      // too slow :-)
      _log.trace(file.toString()+": passivated session not found");
    }
    catch (IOException e)
    {
      _log.warn(file.toString()+": problem restoring passivated session", e);
    }
    catch (ClassNotFoundException e)
    {
      _log.warn(file.toString()+": could not restore passivated session", e);
    }

    return impl;
  }

  public Collection
    findTimedOut(long now, Collection collection)
  {
    // TODO - if a number of nodes all do this at the same time,
    // things will get tricky - consider...
    File[] files=_dir.listFiles();
    int suffixLen=_suffix.length();
    for (int i=0;i<files.length;i++)
    {
      File file=files[i];
      long timeOutAt=file.lastModified();
      if (timeOutAt<=now) // session in file has timed out...
      {
	String name=file.getName();
	String id=name.substring(0, name.length()-suffixLen);
	_log.trace(id+" : file's lastModified indicates expiry: "+timeOutAt+"<="+now);
	collection.add(id);
      }
    }
    return collection;
  }

  public boolean isElected(){return false;}
  public boolean standUp(){return false;}
  public void standDown(){}
}
