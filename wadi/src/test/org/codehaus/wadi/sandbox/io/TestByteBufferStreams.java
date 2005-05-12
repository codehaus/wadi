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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import junit.framework.TestCase;

public class TestByteBufferStreams extends TestCase {

    public TestByteBufferStreams(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testByteBufferInputStream() throws Exception {
        
        
        int capacity=256; //8092; // must be divisible by 4 !
        // create a byte array
        byte[] bytesIn=new byte[capacity];
        // initialise it with testable values
        for (int i=0; i<capacity; i++)
            bytesIn[i]=(byte)(i%256);
        
        // create input and output queues
        LinkedQueue inputQueue=new LinkedQueue();
        LinkedQueue outputQueue=new LinkedQueue();
        // put the queues into the stream
        InputStream is=new ByteBufferInputStream(inputQueue, outputQueue);

        // try exhausting a single ByteBuffer
        
        // allocate a ByteBuffer
        ByteBuffer buffer=ByteBuffer.allocateDirect(capacity);
        // copy byte array into ByteBuffer
        buffer.put(bytesIn);
        // prepare it for reading
        buffer.flip();
        // push the ByteBuffer onto the Stream's input
        inputQueue.put(buffer);

        // read it all out and check it for validity
        {
            byte[] bytesOut=new byte[capacity];
            int bytesRead=0;
            while((bytesRead+=is.read(bytesOut))<capacity);
            assertTrue(bytesRead==capacity);
            assertTrue(Arrays.equals(bytesIn, bytesOut));
        }
        // check the state of the queues...
        assertTrue(inputQueue.isEmpty()); // no input left
        assertTrue(outputQueue.take()!=null); // 1 buffer in output
        assertTrue(outputQueue.isEmpty());
        
        // OK - now lets see if rollover works OK...
        
        int rolloverCapacity=capacity/4;
        for (int i=0; i<4; i++) {
            buffer=ByteBuffer.allocateDirect(rolloverCapacity);
            buffer.put(bytesIn, i*rolloverCapacity, rolloverCapacity);
            buffer.flip();
            inputQueue.put(buffer);
        }
        // read it all out and check it for validity
        {
            byte[] bytesOut=new byte[capacity];
            int bytesRead=0;
            while((bytesRead+=is.read(bytesOut))<capacity);
            assertTrue(bytesRead==capacity);
            assertTrue(Arrays.equals(bytesIn, bytesOut));
        }        
        assertTrue(inputQueue.isEmpty()); // no input left
        assertTrue(outputQueue.take()!=null); // 4 buffers in output
        assertTrue(outputQueue.take()!=null); // 3 buffers in output
        assertTrue(outputQueue.take()!=null); // 2 buffers in output
        assertTrue(outputQueue.take()!=null); // 1 buffer in output
        assertTrue(outputQueue.isEmpty());
        
    }
}
