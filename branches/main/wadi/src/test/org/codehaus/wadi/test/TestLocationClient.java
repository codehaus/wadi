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

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;

public class
  TestLocationClient
{

  public static class Client
    implements Runnable
  {
    protected final int _iters;
    protected final int _port;
    protected final InetAddress _address;
    protected final MulticastSocket _socket;
    protected final String _message;


    public Client(int iters, InetAddress address, int port, MulticastSocket socket, String message)
    {
      _iters=iters;
      _address=address;
      _port=port;
      _socket=socket;
      _message=message;
    }

    public void run()
    {
      for (int i=0;i<_iters;i++)
      {
	try
	{
	_socket.send(new DatagramPacket(_message.getBytes(),
					_message.length(),
					_address,
					_port));
	}
	catch (IOException e)
	{
	  e.printStackTrace();
	}
      }
    }
  }


  public static void
    main(String[] args)
  {
    try
    {
      int port=6789;
      MulticastSocket socket= new MulticastSocket(port);
      InetAddress address=InetAddress.getByName("228.5.6.7");

      socket.setLoopbackMode(false);
      socket.joinGroup(address);

      String message=args[0];

      int numThreads=100;
      int numIters=100;
      Thread[] threads=new Thread[numThreads];
      for (int i=0;i<numThreads;i++)
	threads[i]=new Thread(new Client(numIters, address, port, socket, message));

      for (int i=0;i<numThreads;i++)
	threads[i].start();

      for (int i=0;i<numThreads;i++)
	threads[i].join();

      socket.leaveGroup(address);
    }
    catch (Exception e)
    {
      e.printStackTrace(System.err);
    }
  }
}
