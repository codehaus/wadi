
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

package org.codehaus.wadi.impl.sync;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.AsyncToSyncAdaptor;
import org.codehaus.wadi.HttpSessionImpl;
import org.codehaus.wadi.HttpSessionImplFactory;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.StreamingStrategy;

// TODO - maybe use NIO/AIO as discussed with Jeremy.
// TODO - could our connection be to a filedescriptor instead of another node ?
// TODO - insertion/removal of locks in lockMap NYI

/**
 * Encapsulates mechanism for synchronous i/emmigration of sessions
 * between WADI peers.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  MigrationService
  implements org.codehaus.wadi.MigrationService
{
  protected AsyncToSyncAdaptor _adaptor=new AsyncToSyncAdaptor();
  public AsyncToSyncAdaptor getAsyncToSyncAdaptor(){return _adaptor;}

  protected StreamingStrategy _streamingStrategy;
  protected Manager           _manager;
  protected Client            _client=new Client();
  protected Server            _server=new Server();

  protected Map _sessions;
  public Map getHttpSessionImplMap(){return _sessions;}
  public void setHttpSessionImplMap(Map sessions){_sessions=sessions;}

  public StreamingStrategy getStreamingStrategy(){return _streamingStrategy;}
  public void setStreamingStrategy(StreamingStrategy strategy){_streamingStrategy=strategy;}

  public Manager getManager(){return _manager;}
  public void setManager(Manager manager){_manager=manager;}


  public org.codehaus.wadi.MigrationService.Server getServer(){return _server;}
  public org.codehaus.wadi.MigrationService.Client getClient(){return _client;}

  /**
   * Encapsulates an InetAddress and port, for synchronous
   * SocketServer usage, within a Destination, so that it can be
   * passed around opaquely.
   *
   */
  static class Destination
    implements javax.jms.Destination, Serializable
  {
    InetAddress _address;
    int         _port;
  }

  public class
    Client
    implements org.codehaus.wadi.MigrationService.Client
  {
    protected final Log _log=LogFactory.getLog(getClass());

    protected int         _port=0;	// any port
    protected InetAddress _address; // null seems to work fine as default interface

    public boolean
      emmigrate(Collection candidates, long timeout, javax.jms.Destination dst)
    {
      _log.error("NYI");
      return false;
    }

    public boolean
      immigrate(String realId, HttpSessionImpl placeholder, long timeout, javax.jms.Destination dst)
    {
      _log.error("NYI");
      return false;
    };

    public boolean
      emmigrate(Map map, Collection candidates, long timeout, Destination destination, boolean sync)
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

	  socket=new Socket(destination._address, destination._port, _address, _port);
	  // TODO - do we need a timeout ? - YES
	  if (_log.isTraceEnabled()) _log.trace(socket+" -> "+destination);
	  ObjectOutput os=new ObjectOutputStream(socket.getOutputStream());
	  ObjectInput  is=new ObjectInputStream(socket.getInputStream());
	  //	  ObjectOutput ss=_streamingStrategy.getOutputStream(socket.getOutputStream());
	  ObjectOutput ss=os;

	  // (1) send PREPARE()
	  os.writeBoolean(sync);
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
	    _log.info("SESSION:"+impl);
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
  }

  public class
    Server
    implements org.codehaus.wadi.MigrationService.Server, Runnable
  {
    protected final Log _log=LogFactory.getLog(getClass());

    protected boolean                _running;
    protected Thread                 _thread;
    protected Destination            _destination;
    protected int                    _backlog=16; // TODO - is this a useful default
    protected ServerSocket           _socket;
    protected int                    _timeout=2000;

    protected HttpSessionImplFactory _factory;
    public HttpSessionImplFactory getHttpSessionImplFactory(){return _factory;}
    public void setHttpSessionImplFactory(HttpSessionImplFactory factory){_factory=factory;}

    public void
      start()
    {
      if (_destination==null) _destination=new Destination();
      try
      {
	if (_destination._address==null)
	{
	  _destination._address=InetAddress.getLocalHost();
	  //_address=InetAddress.getByName("localhost");
	}

	if (_log.isDebugEnabled()) _log.debug("starting: "+_destination._address+":"+(_destination._port==0?"<anonymous>":""+_destination._port));

	// initialise dependant resources...
	_socket=new ServerSocket(_destination._port, _backlog, _destination._address);
	_socket.setSoTimeout(_timeout);
	_destination._port=_socket.getLocalPort();

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
    {
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
	if (_log.isDebugEnabled()) _log.debug("stopped: "+_destination._address+":"+_destination._port);
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
      public void run(){immigrate(_socket);}
    }

    public void
      immigrate(Socket socket)
    {
      boolean ok=true;
      boolean commit=false;
      int numSessions=0;
      Collection candidates=new ArrayList(numSessions);
      boolean sync=false;

      try
      {
	ObjectOutput os=new ObjectOutputStream(socket.getOutputStream());
	ObjectInput  is=new ObjectInputStream(socket.getInputStream());
	//	ObjectInput  ss=_streamingStrategy.getInputStream(socket.getInputStream());
	ObjectInput ss=is;


	// (1) receive PREPARE()
	sync=is.readBoolean();
	numSessions=is.readInt(); // how many sessions ?
	for (int i=numSessions; i>0; i--)
	{
	  HttpSessionImpl impl=_factory.create();
	  impl.readContent(ss); // demarshall session off wire
	  impl.getRWLock().setPriority(HttpSessionImpl.EMMIGRATION_PRIORITY); // TODO - does priority matter here?
	  impl.getContainerLock().acquire();
	  impl.setWadiManager(_manager);
	  _sessions.put(impl.getRealId(), impl);
	  candidates.add(impl);
	}

	// (2) send return code from PREPARE()
	os.writeBoolean(true);
	os.flush();

	// (3) receive COMMIT()
	ok=is.readBoolean();

	commit=ok;
      }
      catch (ClassNotFoundException e)
      {
	_log.warn("unexpected problem immigrating sessions - this should not happen - no immigration will have occurred", e);
      }
      catch (InterruptedException e)
      {
	_log.warn("unexpected problem immigrating sessions - thread interrupted - no immigration will have occurred", e);
      }
      catch (IOException e)
      {
	_log.warn("unexpected problem immigrating sessions - target node or comms failure ? - no immigration will have occurred", e);
      }
      finally
      {
	if (commit)
	{
	  if (_log.isDebugEnabled())
	    _log.debug(numSessions+" immigration[s] (peer: "+socket.getRemoteSocketAddress()+":"+socket.getPort()+")");
	}
	else
	{
	  // rollback...
	  _sessions.values().removeAll(candidates);
	  _log.warn("failed to immigrate "+numSessions+" sessions - rolled back");
	}

	for (Iterator i=candidates.iterator(); i.hasNext(); )
	{
	  HttpSessionImpl impl=(HttpSessionImpl)i.next();
	  impl.getContainerLock().release();

	  if (sync && candidates.size()==1)
	    _adaptor.receive(impl, impl.getRealId(), 2000L); // parameterise - TODO

	}

	try{socket.close();}catch(Exception e){_log.warn("problem closing socket",e);}
      }

    }

    public void setDestination(javax.jms.Destination destination) {assert destination instanceof Destination;_destination=(Destination)destination;}
    public javax.jms.Destination getDestination() {return _destination;}
  }
}
