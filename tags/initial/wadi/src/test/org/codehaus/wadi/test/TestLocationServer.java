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
import org.codehaus.wadi.shared.DiscoveryService;

import java.io.IOException;

public class
  TestLocationServer
//  extends LocationService
//  implements Runnable
{
//   protected long   _timeout=2000; // TODO - 0does not quit properly
//   protected String _httpIpAddress;
//   protected String _httpPort;

//   public
//     TestLocationServer(InetAddress serverIpAddress, int serverPort,
// 		       long timeout, String httpIpAddress, String httpPort)
//   {
//     super(serverIpAddress, serverPort);
//     _timeout=timeout;
//     _httpIpAddress=httpIpAddress;
//     _httpPort=httpPort;
//   }

//   public void
//     run()
//   {
//     while (true)
//     {
//       if (_timeout==0) Thread.yield(); // prevent a tight loop...
//       processMulticast(receiveMulticast(_timeout), _timeout, _httpIpAddress, _httpPort);
//     }
//   }

//   public boolean
//     isOwner(String id)
//   {
//     System.out.println("am I the owner: "+id);
//     return true;
//   }

//   public static void
//     main(String args[])
//   {
//     try
//     {
//       TestLocationServer server= new TestLocationServer(InetAddress.getByName("228.5.6.7"),
// 							6789,
// 							2000,
// 							args[0],
// 							args[1]);

//       server.start();
//       server.run();
//       server.stop();
//     }
//     catch (Exception e)
//     {
//       e.printStackTrace(System.err);
//     }
//  }
}
