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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.shared.HttpSessionImpl;
import org.codehaus.wadi.shared.PassivationStrategy;
import org.codehaus.wadi.shared.SerializableContent;


// TODO - should use NIO all the way through. Sessions should be
// written into a byte[] that is just stuck on the Channel etc... This
// will tie in nicely with replication and migration which will be
// done in the same way.

// TODO - what should we do with session files that will no longer
// load (e.g. due to incompatible classes)?

// TODO - broken files should not be removed, but moved to one side...

// TODO - do we need to worry about system limits on file size/numbers ?

// TODO - we could isolate and partition this data onto many different NFS
// servers for resilience and scaling etc...

// TODO this needs tidying up and double checking...

// TODO

// Client sends concurrent requests to nodes A and B asking for
// passivated session S. As requests enter filter A wins session lock
// and activates session. B loses. What does B now do ???

// 1. give up :-(
// 2. send a broadcast requiring migration of S to B - no good, the existing req on A must be serviced.
// 3. send a broadcast enquiring as to the whereabouts of S, then proxy the request to that node.

// Answer - (3)

// Questions:

// 1. There is a race condition here, B may send the enquiry after
// losing the race to S on disc, but before A has finished activating
// it - it might be a very large session.

// As soon as A wins the lock it must be prepared to answer B about
// the ownership of S.

// By the time the proxied request arrives from B, A must already have
// an object in the session table and own a W lock on it, thus holding
// B's request up at entrance to container whilst it activates the
// session. - should the lock not live in the session ?

// 2. if we use the filter to 'proxy' the request, whose 'routing
// info' will end up on the session id - A or B ?



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
  FilePassivationStrategy
  implements PassivationStrategy
{
  protected final Log      _log    =LogFactory.getLog(getClass());
  protected final String   _suffix =".ser";
  protected final File     _dir;
  protected final File     _dgcFile;
  protected       FileLock _dgcLock;

  public
    FilePassivationStrategy(String dir)
    {
      this(new File(dir));
    }

  public
    FilePassivationStrategy(File dir)
    {
      _dir=dir;

      if (!_dir.exists() && !_dir.mkdirs())
	throw new IllegalArgumentException("could not create dir: "+_dir);

      if (!(_dir.exists() &&
	    _dir.isDirectory() &&
	    _dir.canRead() &&
	    _dir.canWrite()))
	throw new IllegalArgumentException("bad dir (is it rw?): "+_dir);

      _dgcFile=new File(_dir, "housekeeper.lck");
    }

  public boolean
    passivate(HttpSessionImpl impl)
    {
      String id=impl.getId();
      File file=null;
      FileLock lock=null;
      try
      {
	file=new File(_dir, id+_suffix);
	FileOutputStream fos=new FileOutputStream(file);
	lock=fos.getChannel().lock();
	_log.trace(file.toString()+": passivating : "+impl);
	ObjectOutputStream oos=new ObjectOutputStream(fos);
	impl.writeContent(oos);
	oos.flush();
	oos.close();

	long willTimeOutAt=impl.getLastAccessedTime()+(impl.getMaxInactiveInterval()*1000);
	file.setLastModified(willTimeOutAt);
	return true;
      }
      catch (Exception e)
      {
	_log.warn(file.toString()+": problem storing passivated session", e);
	return false;
      }
      finally
      {
	try
	{
	  if (lock!=null) lock.release();
	}
	catch (IOException e)
	{
	  _log.warn(file.toString()+": problem releasing passivation resource", e);
	}
      }
    }

  public boolean
    activate(String id, HttpSessionImpl impl)
    {
      File file=null;
      RandomAccessFile raf=null;
      try
      {
	file=new File(_dir, id+_suffix);
	// quick exit - saves expensive allocation and stack unwinding
	// if file not present..
	if (!file.exists())
	  return false;
	else
	{
	  FileLock lock=null;
	  try
	  {
	    raf=new RandomAccessFile(file, "rw");
	    lock=raf.getChannel().lock();
	    FileInputStream fis=new FileInputStream(file);
	    ObjectInputStream ois=new ObjectInputStream(fis);
	    impl.readContent(ois);
	    ois.close();
	    _log.trace(file.toString()+": activating : "+impl);
	    return true;
	  }
	  finally
	  {
	    try
	    {
	      if (lock!=null) lock.release();
	      if (raf!=null) raf.close();
	      file.delete();
	    }
	    catch (IOException e)
	    {
	      _log.warn(file.toString()+": problem releasing passivation resource", e);
	    }
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

      return false;
    }

  /**
   * Find and return the ids of all passivated sessions that have
   * timed out.
   *
   * @param now a <code>long</code> value
   * @param collection a <code>Collection</code> value
   * @return a <code>Collection</code> value
   */
  public Collection
    findTimedOut(long now, Collection collection)
    {
      // TODO - if a number of nodes all do this at the same time,
      // things will get tricky - consider...
      int suffixLen=_suffix.length();
      File[] files=_dir.listFiles();
      if (files!=null)
	for (int i=0;i<files.length;i++)
	{
	  File file=files[i];

	  if (file.equals(_dgcFile)) continue; // ignore housekeeper coordination lock file

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

  public boolean isElected(){return _dgcLock!=null;}

  public boolean
    standUp()
    {
      _log.trace("standing for election");
      FileChannel channel=null;
      try
      {
	channel=new RandomAccessFile(_dgcFile, "rw").getChannel();
	_dgcLock=channel.tryLock(); // take exclusive lock
      }
      catch (Exception e)
      {
	_log.warn("problem acquiring distributed garbage collection lock", e);
      }

      if(_dgcLock!=null)
      {
	// keep channel open to keep lock alive...
	_log.trace("elected to distributed garbage collection duties");
	return true;
      }
      else
      {
	try
	{
	  channel.close();
	}
	catch (Exception e)
	{
	  _log.warn("problem tidying up file lock... ",e);
	}

	_log.trace("not elected");
	return false;
      }
    }

  public void
    standDown()
    {
      try
      {
	if (_dgcLock!=null)
	{
	  _log.trace("standing down from distributed garbage collection duties");
	  _dgcLock.release();
	  _dgcLock.channel().close();
	  _dgcLock=null;
	}
      }
      catch (Exception e)
      {
	_log.warn("problem releasing distributed garbage collection lock", e);
      }
    }
}
