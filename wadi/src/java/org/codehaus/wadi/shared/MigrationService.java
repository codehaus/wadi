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

package org.codehaus.wadi.shared;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO - should use NIO as discussed with Jeremy.
// TODO - could our connection be to a filedescriptor instead of another node ?
// TODO - insertion/removal of locks in lockMap NYI

public class
  MigrationService
{
  public static class
    Client
  {
    protected final Log _log=LogFactory.getLog(getClass());

    protected int         _port=0;	// any port
    protected InetAddress _address; // null seems to work fine as default interface

    public HttpSessionImpl
      run(Map sessions, Map locks, String id, InetAddress remoteAddress, int remotePort)
    {
      Sync lock=null;
      boolean acquired=false;
      try
      {
	if (_address==null)
	  _address=InetAddress.getLocalHost();

	Socket socket=new Socket(remoteAddress, remotePort, _address, _port);
	// do we need a timeout ?
	_log.trace("socket: "+socket);
	ObjectOutputStream os=new ObjectOutputStream(socket.getOutputStream());
	ObjectInputStream  is=new ObjectInputStream(socket.getInputStream());
	// acquire container lock on session id
	// send migration request to session source
	os.writeObject("migrate");
	os.writeObject(id);
	os.flush();
	// demarshall session off wire
	HttpSessionImpl session=(HttpSessionImpl)is.readObject();
	_log.trace("received migrated session: "+session);
	// send commit message
	os.writeObject("commit");
	os.flush();
	// insert session
	sessions.put(id, session); // TODO - needs wrapping in facade etc..
	socket.close();
	return session;
      }
      catch (UnknownHostException e)
      {
	_log.warn("unexpected problem resolving client interface", e);
      }
      catch (IOException e)
      {
	_log.warn("problem migrating session", e);
      }
      catch (ClassNotFoundException e)
      {
	_log.warn("problem migrating session", e);
      }
      finally
      {
	// release lock
	if (acquired)
	  lock.release();
      }

      return null;
    }

  }

  public static class
    Server
    implements Runnable
  {
    protected final Log _log=LogFactory.getLog(getClass());

    protected boolean      _running;
    protected Thread       _thread;
    protected int          _port=0; // any free port
    protected InetAddress  _address;
    protected int          _backlog=16;	// TODO - is this a useful default
    protected ServerSocket _socket;
    protected int          _timeout=2000;

    protected final Map _locks;
    protected final Map _sessions;

    public int getPort(){return _port;}
    public void setPort(int port){_port=port;}

    public InetAddress getAddress(){return _address;}
    public void setAddress(InetAddress address){_address=address;}

    public Server(Map sessions, Map locks)
    {
      _sessions=sessions;
      _locks=locks;
    }

    public void
      start()
    {
      _log.trace("starting");
      try
      {
	if (_address==null)
	  _address=InetAddress.getLocalHost();

	// initialise dependant resources...
	_socket=new ServerSocket(_port, _backlog, _address);
	_socket.setSoTimeout(_timeout);
	_log.trace("socket: "+_socket);
	_port=_socket.getLocalPort();

	// start...
	_running=true;
	_thread=new Thread(this);
	_thread.start();
	_log.trace("started");
	return;
      }
      catch (UnknownHostException e)
      {
	_log.warn("unexpected problem resolving listening interface", e);
      }
      catch (IOException e)
      {
	_log.warn("unexpected problem creating server socket", e);
      }

      // we shouldn't get to here...
      _log.warn("did not start");
    }

    public void
      stop()
    {
      _log.trace("stopping");
      _running=false;
      _thread.interrupt();
      try
      {
	_thread.join();
	_thread=null;
	_socket.close();
	_socket=null;
	// we need to join all our subthreads :-(
	_log.trace("stopped");
      }
      catch (InterruptedException e)
      {
	_log.warn("unexpectedly interrupted whilst stopping");
	// TODO - interrupted()?
      }
      catch (IOException e)
      {
	_log.warn("could not close server socket");
      }
    }

    public void
      run()
    {
      try
      {
	while (_running)
	{
	  try
	  {
	    if (_timeout==0) Thread.yield();
	    new Thread(new Migration(_socket.accept())).start();
	    // TODO - remember threads started and join them on stop() ?
	    // run this request on current thread, starting a new one to listen()
	  }
	  catch (SocketTimeoutException ignore)
	  {
	  }
	}
      }
      catch (IOException e)
      {
	_log.warn("unexpected io problem - stopping");
      }
    }

    public class
      Migration
      implements Runnable
    {
      protected final Socket _socket;
      public Migration(Socket socket){_socket=socket;}
      public void run(){migrate(_socket);}
    }

    public void
      migrate(Socket socket)
    {
      Sync lock=null;
      boolean acquired=false;
      try
      {
	ObjectOutputStream os=new ObjectOutputStream(socket.getOutputStream());
	ObjectInputStream  is=new ObjectInputStream(socket.getInputStream());
	// receive migration request from target
	String method=(String)is.readObject();
	// method==migrate
	String id=(String)is.readObject();
	// acquire container lock on session id
	// marshall session onto wire
	HttpSessionImpl impl=(HttpSessionImpl)_sessions.get(id); // TODO - what if session is not there ?
	os.writeObject(impl);
	os.flush();
	// receive commit message
	String commit=(String)is.readObject();
	// commit=="commit"
	// remove session
	_sessions.remove(id); // TODO - recycle

	socket.close();		// TODO - should we do this ?
      }
      catch (IOException e)
      {
	_log.warn("problem migrating session", e);
      }
      catch (ClassNotFoundException e)
      {
	_log.warn("problem migrating session", e);
      }
      finally
      {
	// release lock
	if (acquired)
	  lock.release();
      }
    }
  }
}
