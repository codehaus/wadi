/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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

 package org.codehaus.wadi.old.test;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
 * Multicast related tests
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestMulticastServer
  implements Runnable
{
  protected MulticastSocket _socket;

  public
    TestMulticastServer(String ip, int port)
    throws Exception
  {
    InetAddress address = InetAddress.getByName(ip);
    _socket = new MulticastSocket(port);
    _socket.joinGroup(address);
  }

  public void
    run()
  {
    try
    {
      System.out.println("starting: "+_socket);
      byte[] buffer=new byte[1024];
      while (true)
      {
	DatagramPacket packet=new DatagramPacket(buffer, buffer.length);
	_socket.receive(packet);
	System.out.println("received: "+new String(packet.getData(), packet.getOffset(), packet.getLength()));
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public static void
    main(String args[])
    throws Exception
  {
    String address="228.5.6.7";
    int port=6789;

    TestMulticastServer server= new TestMulticastServer(address, port);
    server.run();
  }
}
