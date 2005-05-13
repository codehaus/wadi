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
package org.codehaus.wadi.sandbox.io;

// This is largely copied from Jetty's SocketChannelListener - cheers Greg ! (Jetty is ASF2.0)

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

public class TestNIO extends TestCase {

    protected final Log _log=LogFactory.getLog(getClass());
    
    public TestNIO(String name) {
        super(name);
    }

    protected final int _port=8888;
    protected NIOServer _server;
    
    protected void setUp() throws Exception {
        super.setUp();
        _server=new NIOServer(new InetSocketAddress(InetAddress.getLocalHost(), _port), 4096, 4);
        _server.start();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        _server.stop();
    }

//    public void testWrite() throws Exception {
//        assertTrue(true);
//        
//        int size=1024;
//        byte[] bytes=new byte[size];
//        for (int i=0; i<size; i++)
//            bytes[i]=(byte)i;
//        File file=File.createTempFile("wadi-", ".tst");
//        FileOutputStream fos=new FileOutputStream(file);
//        fos.write(bytes);
//        fos.close();
//
//        _log.info("File: "+file);
//        
//        assertTrue(file.length()==size);
//        FileInputStream fis= new FileInputStream(file);
//        FileChannel fc=fis.getChannel();
//        MappedByteBuffer mbb=fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
//    }
    
    public void testConnect() throws Exception {
//        long start=System.currentTimeMillis();
//        int count=500;
//        InetAddress localhost=InetAddress.getLocalHost();
//        for (int i=0; i<count; i++) {
//            new Socket(localhost, _port);
//        }
//        long elapsed=System.currentTimeMillis()-start;
//        _log.info("elapsed: "+(count*1000/elapsed)+" sockets/second");
//        Thread.sleep(5000);
        
        Thread.sleep(10*60*1000);
    }
}
