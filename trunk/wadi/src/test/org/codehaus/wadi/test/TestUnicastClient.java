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

 package org.codehaus.wadi.test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
 * Test unicast client to multicast server
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  TestUnicastClient
  implements Runnable
{
  protected DatagramSocket  _socket;
  protected DatagramPacket  _packet;

  public
    TestUnicastClient(String ip, int port, String message)
    throws Exception
  {
    byte[] bytes=message.getBytes();
    _socket = new DatagramSocket();
    _packet = new DatagramPacket(bytes, 0, bytes.length);
    _packet.setAddress(InetAddress.getByName(ip));
    _packet.setPort(port);
  }

  public void
    run()
  {
    try
    {
      _socket.send(_packet);
      System.out.println("sent: "+new String(_packet.getData(), _packet.getOffset(), _packet.getLength()));
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
    String message=args[0];

    TestUnicastClient client= new TestUnicastClient(address, port, message);
    client.run();
  }
}
