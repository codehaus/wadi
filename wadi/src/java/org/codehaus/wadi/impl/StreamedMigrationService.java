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

package org.codehaus.wadi.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;

import org.codehaus.wadi.HttpSessionImpl;
import org.codehaus.wadi.MigrationService;

// TODO - maybe use NIO/AIO as discussed with Jeremy.
// TODO - could our connection be to a filedescriptor instead of another node ?

/**
 * Encapsulates mechanism for synchronous i/emmigration of sessions
 * between WADI peers.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  StreamedMigrationService
  extends AbstractMigrationService
{
  protected Client _client=new Client();
  protected Server _server=new Server();

  public MigrationService.Server getServer(){return _server;}
  public MigrationService.Client getClient(){return _client;}

  public void setTimeout(int timeout) {((Server)getServer()).setTimeout(timeout);}
  public int getTimeout() {return ((Server)getServer()).getTimeout();}

  public class
    Client
    extends AbstractMigrationService.Client
    {
      protected int         _port=0;	// any port
      protected InetAddress _address; // null seems to work fine as default interface

      public void setAddress(InetAddress address){_address=address;}
      public void setPort(int port){_port=port;}

      public boolean
	immigrateSingleSession(String realId, HttpSessionImpl placeholder, long timeout, Destination dst)
	{
	  Destination src=_server.getDestination();
	  String correlationId=realId+"-"+src.toString();
	  _log.trace("sending MigrationRequest to:"+dst);
	  Object sync=_adaptor.send(_manager.getCluster(),
				    new StreamedMigrationRequest(realId, src, timeout),
				    correlationId,
				    timeout,
				    null,
				    dst,
				    placeholder);

	  if (sync!=null)
	    // we wait until we can get this lock before continuing...
	    synchronized (sync)
	    {
	      return placeholder.getRealId()!=null;
	    }
	  else
	    return false;
	};

      public boolean
	emmigrateMultipleSessions(Map map, Collection candidates, long timeout, Destination destination)
	{
	  Socket socket=null;
	  boolean ok=true;
	  int locked=0;		// in case we are interrupted whilst locking candidates
	  boolean commit=false;
	  int numSessions=0;

	  try
	  {
	    // only try to migrate those candidates that we can lock -
	    // i.e. are not in use...
	    for (Iterator i=candidates.iterator(); i.hasNext(); locked++)
	    {
	      HttpSessionImpl impl=(HttpSessionImpl)i.next();
	      impl.getRWLock().setPriority(HttpSessionImpl.EMMIGRATION_PRIORITY);
	      if (!impl.getContainerLock().attempt(timeout)) // InterruptedException here
		i.remove();
	    }

	    numSessions=candidates.size();
	    if (numSessions>0)
	    {
	      if (_address==null)
	      {
		_address=InetAddress.getLocalHost();
		//_address=InetAddress.getByName("localhost");
	      }

	      InetSocketAddressDestination dest=(InetSocketAddressDestination)destination;
	      socket=new Socket(dest.getAddress(), dest.getPort(), _address, _port);
	      // TODO - do we need a timeout ? - YES
	      if (_log.isTraceEnabled()) _log.trace(socket+" -> "+destination);
	      ObjectOutput os=new ObjectOutputStream(socket.getOutputStream());
	      ObjectInput  is=new ObjectInputStream(socket.getInputStream());
	      //	  ObjectOutput ss=_streamingStrategy.getOutputStream(socket.getOutputStream());
	      ObjectOutput ss=os;

	      // (1) send PREPARE()
	      os.writeInt(numSessions);
	      os.flush();
	      for (Iterator i=candidates.iterator(); i.hasNext(); )
		((HttpSessionImpl)i.next()).writeContent(ss); // ClassNotFoundException here
	      ss.flush();

	      // (2) receive return code from PREPARE()
	      ok=is.readBoolean(); // protocol may later allow partial success
	      if (ok) map.values().removeAll(candidates); // release session ownership this end

	      // (3) send COMMIT()
	      os.writeBoolean(ok);
	      os.flush();
	    }

	    commit=ok;
	  }
	  catch (ClassNotFoundException e)
	  {
	    _log.warn("unexpected problem emmigrating sessions - this should not happen - no emmigration will have occurred", e);
	  }
	  catch (InterruptedException e)
	  {
	    _log.warn("unexpected problem emmigrating sessions - thread interrupted - no emmigration will have occurred", e);
	  }
	  catch (UnknownHostException e)
	  {
	    _log.warn("unexpected problem emmigrating sessions - could not resolve target interface - no emmigration will have occurred", e);
	  }
	  catch (IOException e)
	  {
	    _log.warn("unexpected problem emmigrating sessions to "+destination+"- target node or comms failure ? - no emmigration will have occurred", e);
	  }
	  // what about timeout...
	  finally
	  {
	    if (commit)
	    {
	      // should I print out all session ids - so we have a record ?
	      if (_log.isDebugEnabled())
		_log.debug(numSessions+" emmigration[s] (peer: "+destination+")");
	    }
	    else
	    {
	      // rollback...
	      for (Iterator i=candidates.iterator(); i.hasNext(); )
	      {
		HttpSessionImpl impl=(HttpSessionImpl)i.next();
		map.put(impl.getRealId(), impl);
	      }
	    }

	    // release impls' locks
	    for (Iterator i=candidates.iterator(); i.hasNext() && locked>0; locked--)
	      ((HttpSessionImpl)i.next()).getContainerLock().release();

	    if (socket!=null)
	    {
	      try{socket.close();}catch(Exception e){_log.warn("problem closing socket",e);}
	      socket=null;
	    }
	  }

	  return ok;
	}

      //----------------------------------------

      public Object
	emmigrateSingleSession(StreamedMigrationRequest request, HttpSessionImpl impl, String correlationID, Destination source)
	{
	  Socket socket=null;
	  boolean ok=true;

	  String id=impl.getRealId();
	  InetSocketAddressDestination dest=(InetSocketAddressDestination)source;

	  try
	  {
	    if (_address==null)
	    {
	      _address=InetAddress.getLocalHost();
	      //_address=InetAddress.getByName("localhost");
	    }

	    socket=new Socket(dest.getAddress(), dest.getPort(), _address, _port);
	    // TODO - do we need a timeout ? - YES
	    if (_log.isTraceEnabled()) _log.trace(socket+" -> "+dest);
	    ObjectOutput os=_streamingStrategy.getOutputStream(socket.getOutputStream());
	    ObjectInput  is=_streamingStrategy.getInputStream(socket.getInputStream());
	    // send the code to deal with this transmission
	    os.writeObject(new SingleSessionImmigrationProcessor());
	    os.flush();
	    // send the session id, so placeholder may be found
	    os.writeObject(id);
	    os.flush();
	    // send the session content...
	    impl.writeContent(os);
	    os.flush();
	    // read remote status...
	    ok=is.readBoolean();
	    // write our own status
	    os.writeBoolean(ok);
	    os.flush();
	    if (_log.isDebugEnabled()) _log.debug(id+": emmigration (peer: "+dest+")");
	  }
	  catch (UnknownHostException e)
	  {
	    _log.warn(id+": could not reply to source of request", e);
	  }
	  catch (IOException e)
	  {
	    _log.warn(id+": emmigration connection broken - rolling back", e);
	    ok=false;
	  }
	  catch (ClassNotFoundException e)
	  {
	    _log.warn(id+": emmigration class mismatch - version/security problem? - rolling back", e);
	    ok=false;
	  }
	  finally
	  {
	    if (socket!=null)
	    {
	      try{socket.close();}catch(Exception e){_log.warn(id+": problem closing socket",e);}
	      socket=null;
	    }
	  }

	  return ok?Boolean.TRUE:Boolean.FALSE;
	}

      //----------------------------------------
    }

  public class
    Server
    extends AbstractMigrationService.Server
    implements Runnable
    {
      protected boolean                      _running;
      protected Thread                       _thread;
      protected InetSocketAddressDestination _destination;
      protected int                          _backlog=16; // TODO - is this a useful default
      protected ServerSocket                 _socket;
      protected int                          _timeout=2000;

      public void setDestination(Destination destination) {_destination=(InetSocketAddressDestination)destination;}
      public Destination getDestination() {return _destination;}

      public void setTimeout(int timeout) {_timeout=timeout;}
      public int getTimeout() {return _timeout;}

      public void
	start()
	throws JMSException
      {
	super.start();

	if (_destination==null) _destination=new InetSocketAddressDestination();
	try
	{
	  if (_destination.getAddress()==null)
	  {
	    _destination.setAddress(InetAddress.getLocalHost());
	    //_address=InetAddress.getByName("localhost");
	  }

	  if (_log.isDebugEnabled()) _log.debug("starting: "+_destination);

	  // initialise dependant resources...
	  _socket=new ServerSocket(_destination.getPort(), _backlog, _destination.getAddress());
	  _socket.setSoTimeout(_timeout);
	  _destination.setPort(_socket.getLocalPort());

	  // start...
	  _running=true;
	  _thread=new Thread(this);
	  _thread.start();
	  if (_log.isDebugEnabled()) _log.debug("started: "+_socket);
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
	throws JMSException
      {
	super.stop();

	if (_log.isDebugEnabled()) _log.debug("stopping: "+_socket);
	_running=false;
	//      try {new Socket(_address, _port).close();} catch (Throwable ignore) {}
	try
	{
	  _thread.join();
	  _thread=null;
	  _socket.close();
	  _socket=null;
	  // we need to join all our subthreads :-(
	  if (_log.isDebugEnabled()) _log.debug("stopped: "+_destination);
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
	      new Thread(new Consumer(_socket.accept())).start();
	      // TODO - remember threads started and join them on stop() ?
	      // run this request on current thread, starting a new one to listen()
	    }
	    catch (SocketTimeoutException ignore)
	    {
	      // ignore
	    }
	  }
	}
	catch (IOException e)
	{
	  _log.warn("unexpected io problem - stopping");
	}
      }

      public class
	Consumer
	implements Runnable
      {
	protected final Socket _socket;
	public Consumer(Socket socket){_socket=socket;}

	public void
	  run()
	{
	  try
	  {
	    ObjectOutput oo=new ObjectOutputStream(_socket.getOutputStream());
	    ObjectInput  oi=new ObjectInputStream(_socket.getInputStream());

	    InetSocketAddressDestination peer=new InetSocketAddressDestination();
	    peer.setAddress(_socket.getInetAddress());
	    peer.setPort(_socket.getPort());
	    ((Processor)oi.readObject()).process(Server.this, oi, oo, peer);

	    oo.close();
	    oi.close();
	  }
	  catch (IOException e)
	  {
	    _log.warn("session immigration connection broken - aborting", e);
	  }
	  catch (ClassNotFoundException e)
	  {
	    _log.warn("unknown session immigration processor - version/security problem?", e);
	  }
	  finally
	  {
	    try{_socket.close();}catch(Exception e){_log.warn("problem closing socket",e);}
	  }
	}
      }

      public void
	immigrateSingleSession(ObjectInput is, ObjectOutput os, Destination peer)
      {
	boolean ok=true;
	String id=null;
	HttpSessionImpl impl=null;

	Object sync=new Object();
	synchronized (sync)
	{
	  try
	  {
	    id=(String)is.readObject();

	    String correlationID=id+"-"+getDestination(); // no need to pass it - we can work it out
	    impl=(HttpSessionImpl)_adaptor.receive(sync, correlationID, _timeout);

	    if (impl==null)
	    {
	      if (_log.isInfoEnabled())
		_log.info(id+": immigration unsuccessful - missed rendezvous");

	      // TODO - it's inefficient to bother reading in the rest
	      // of the message if we have missed our rendezvous - but
	      // possibly simpler than other solutions and should be
	      // exceptional...

	      ok=false;
	      impl=_manager.getHttpSessionImplFactory().create();
	      // return;
	    }
	    else
	      if (_log.isDebugEnabled())
		_log.debug(id+": immigration (peer: "+peer+")");

	    impl.readContent(is);
	    os.writeBoolean(ok);
	    os.flush();
	    ok=is.readBoolean();
	  }
	  catch (IOException e)
	  {
	    _log.warn("session immigration connection broken - rolling back", e);
	  }
	  catch (ClassNotFoundException e)
	  {
	    _log.warn("session immigration class mismatch - version/security problem? - rolling back", e);
	  }

	  if (!ok && impl!=null)
	    _manager._releaseImpl(impl);
	} // sync complete, now originating thread will continue...
      }
  }

  static abstract class
    Processor
    implements Serializable
    {
      public abstract void process(Server server, ObjectInput oi, ObjectOutput oo, Destination peer);
    }

  static class
    SingleSessionImmigrationProcessor
    extends Processor
    {
      public void
	process(Server server, ObjectInput is, ObjectOutput os, Destination peer)
	{
	  server.immigrateSingleSession(is, os, peer);
	}
    }

  static class
    MultipleSessionImmigrationProcessor
    extends Processor
    {
      public void
	process(Server server, ObjectInput oi, ObjectOutput oo, Destination peer)
	{
	}
    }
}
