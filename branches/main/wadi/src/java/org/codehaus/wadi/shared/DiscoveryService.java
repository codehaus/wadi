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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Everyone listens on a well known multicast address:port
 *
 * To locate a session you send a
 * "org.codehaus.wadi,locate,<session-id>" from a unicast socket to
 * this address.
 *
 * The server at this address will check their session inventory and
 * if they are the current owner, send back a
 * "org.codehaus.wadi,locate,<session-id>,<httpAddress>,<httpPort>"
 * packet, via their multicast socket to this unicast socket.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version 1.0
 */
public abstract class
  DiscoveryService
{
  public static class
    Client
  {
    protected final Log         _log=LogFactory.getLog(Client.class);
    protected final InetAddress _address;
    protected final int         _port;
    protected final long        _timeout;

    public
      Client(InetAddress address, int port, long timeout)
    {
      _address=address;
      _port=port;
      _timeout=timeout;
    }

    public String
      run(String request)
    {
      String response=null;
      DatagramSocket socket=null;

      try
      {
	socket=new DatagramSocket();
	socket.setSoTimeout((int)_timeout); // int/millis
	DatagramPacket packet=new DatagramPacket(request.getBytes(), request.length(), _address, _port);
	_log.trace("sending request (to "+_address+":"+_port+"): "+request);
	socket.send(packet);

	byte[] buffer=new byte[1024]; // TODO - big enough?
	long start=System.currentTimeMillis();
	for (long elapsed=0; elapsed<_timeout;)
	{
	  packet.setData(buffer, 0, buffer.length);
	  socket.receive(packet);
	  response=new String(packet.getData(), packet.getOffset(), packet.getLength());
	  if (response!=null && response.startsWith(request)) // TODO - NB response must 'startWith' request...
	  {
	    _log.trace("received well formed response (from "+packet.getAddress()+":"+packet.getPort()+"):"+response);
	    return response;	// TODO - we need an API that allows verification of response
	  }
	  else
	  {
	    _log.warn("received malformed response (from "+packet.getAddress()+":"+packet.getPort()+"):"+response);
	  }
	  elapsed=System.currentTimeMillis()-start;
	}
      }
      catch (SocketTimeoutException ignore)
      {
	_log.trace("no response for: "+request);
      }
      catch (IOException e)
      {
	_log.warn("problem handshaking with server", e);
      }
      finally
      {
	if (socket!=null)
	  socket.close();
      }

      return response;
    }
  }

  public static abstract class
    Server
  {
    protected final Log         _log=LogFactory.getLog(Server.class);
    protected final InetAddress _address;
    protected final int         _port;
    protected final long        _timeout=20000;

    protected volatile boolean  _running=false;

    protected MulticastSocket   _socket=null;
    protected Thread            _thread;

    public
      Server(InetAddress address, int port)
    {
      _address=address;
      _port=port;
    }

    public void
      start()
    {
      _log.trace("starting: "+_address+":"+_port);
      try
      {
	_socket=new MulticastSocket(_port); // 49152-65535
	_socket.joinGroup(_address); // 224.0.0.1-239.255.255.255
	_socket.setLoopbackMode(true); // messages we send should not come back to us
	_socket.setSoTimeout((int)_timeout);
      }
      catch (IOException e)
      {
	_log.warn("could not set up server socket", e);
      }

      _running=true;
      (_thread=new Thread(){public void run(){Server.this.run();}}).start();
    }

    public void
      stop()
    {
      _running=false;
      _thread.interrupt();
      try
      {
	_thread.join();
      }
      catch (InterruptedException e)
      {
	_log.warn("unexpected interruption stopping");
      }

      _thread=null;
      try
      {
	_socket.leaveGroup(_address);
      }
      catch (IOException e)
      {
	_log.warn("unexpected problem stopping");
      }

      _socket.close();
      _socket=null;

      _log.warn("stopped: "+_address+":"+_port);
    }

    public void
      run()
    {
      // N.B. currently a single threaded server - TODO
      byte[] buffer=new byte[256]; // TODO - big enough ?
      DatagramPacket packet=new DatagramPacket(buffer, buffer.length);

      while (_running)
      {
	String request=null;
	packet.setData(buffer, 0, buffer.length);
	try
	{
	  if (_timeout==0) Thread.yield();
	  _socket.receive(packet);
	  request=new String(packet.getData(), packet.getOffset(), packet.getLength());
	  _log.trace("received request (from "+packet.getAddress()+":"+packet.getPort()+"): "+request);
	}
	catch (SocketTimeoutException ignore)
	{
	}
	catch (IOException e)
	{
	  _log.warn("problem receiving packet", e);
	}

	// TODO - run following section on new thread...
	String response=null;
	if (request!=null)
	  response=process(request);

	if (response!=null)
	{
	  packet.setData(response.getBytes(), 0, response.length());
	  // should not need to do this...
	  //	  packet.setAddress(packet.getAddress());
	  //	  packet.setPort(packet.getPort());
	  try
	  {
	    _socket.send(packet);
	  }
	  catch (IOException e)
	  {
	    _log.warn("unexpected problem sending response", e);
	  }
 	  _log.trace("sent response (to "+packet.getAddress()+":"+packet.getPort()+"): "+response);
	}
      }
    }

    public abstract String process(String request);
  }
}
