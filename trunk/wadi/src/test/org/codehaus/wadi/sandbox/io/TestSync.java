package org.codehaus.wadi.sandbox.io;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import junit.framework.TestCase;
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

public class TestSync extends TestCase {
    
    protected Log _log = LogFactory.getLog(getClass());
    
    public TestSync(String name) {
        super(name);
    }
    
    protected Server _server;
    protected Socket _client;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        _server=new Server(new InetSocketAddress(8888), 16, 1);
        _server.start();
        
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        _server.stop();
    }

    public static class MyExecutable implements Executable {
        
        protected static final Log _log=LogFactory.getLog(Executable.class);
        
        public void process(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
            try {
                oos.writeBoolean(true); // ack
                oos.flush();
                ois.close();
                oos.close();
                socket.close();
            } catch (IOException e) {
                _log.warn(e);
            }
        }
        
    }
    
    public void testConnect() throws Exception {

        long start=System.currentTimeMillis();
        
        int count=10000;
        InetAddress localhost=InetAddress.getLocalHost();
        for (int i=0; i<count; i++) {
            Socket socket=new Socket(localhost, 8888);
            ObjectInputStream ois=new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(new MyExecutable());
            assertTrue(ois.readBoolean());
            ois.close();
            oos.close();
            socket.close();
        }
        long elapsed=System.currentTimeMillis()-start;
        _log.info("rate="+(count*1000/elapsed)+" reqs/second");
    }
}
