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

// TODO - maybe use NIO as discussed with Jeremy.
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

    public boolean
      run(Map sessions, Map locks, String id, HttpSessionImpl session, InetAddress remoteAddress, int remotePort)
    {
      Sync lock=null;
      boolean acquired=false;
      Socket socket=null;

      try
      {
	if (_address==null)
	{
	  _address=InetAddress.getLocalHost();
	  //_address=InetAddress.getByName("localhost");
	}

	socket=new Socket(remoteAddress, remotePort, _address, _port);
	// TODO - do we need a timeout ? - YES
	_log.trace(socket+" -> "+remoteAddress+":"+remotePort);
	ObjectOutputStream os=new ObjectOutputStream(socket.getOutputStream());
	ObjectInputStream  is=new ObjectInputStream(socket.getInputStream());
	// acquire container lock on session id
	// send migration request to session source
	os.writeObject("migrate");
	os.writeObject(id);
	os.flush();
	// demarshall session off wire
	session.readContent(is);
	_log.trace("received migrated session: "+session);
	// send commit message
	os.writeBoolean(true);
	os.flush();
	// insert session
	sessions.put(id, session); // TODO - needs wrapping in facade etc..
	// receive commit message
	boolean ok=is.readBoolean();
	assert ok;
	return ok;
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

	if (socket!=null)
	  {
	    try{socket.close();}catch(Exception e){_log.warn("problem closing socket",e);}
	    socket=null;
	  }
      }

      return false;
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
      try
      {
	if (_address==null)
	{
	  _address=InetAddress.getLocalHost();
	  //_address=InetAddress.getByName("localhost");
	}

	_log.debug("starting: "+_address+":"+(_port==0?"<anonymous>":""+_port));

	// initialise dependant resources...
	_socket=new ServerSocket(_port, _backlog, _address);
	_socket.setSoTimeout(_timeout);
	_port=_socket.getLocalPort();

	// start...
	_running=true;
	_thread=new Thread(this);
	_thread.start();
	_log.debug("started: "+_socket);
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
      _log.debug("stopping: "+_socket);
      _running=false;
      //      try {new Socket(_address, _port).close();} catch (Throwable ignore) {}
      try
      {
	_thread.join();
	_thread=null;
	_socket.close();
	_socket=null;
	// we need to join all our subthreads :-(
	_log.debug("stopped: "+_address+":"+_port);
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
	_log.trace("sending migrating session: "+impl);
	impl.writeContent(os);
	os.flush();
	// receive commit message
	boolean ok=is.readBoolean();
	assert ok;
	// remove session
	_sessions.remove(id); // TODO - recycle
	// send commit message
	os.writeBoolean(ok);
	os.flush();
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

	try{socket.close();}catch(Exception e){_log.warn("problem closing socket",e);}
      }
    }
  }
}
