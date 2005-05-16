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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Puttable;

public class ByteArrayInputStream extends InputStream implements Puttable {

    protected final static Log _log=LogFactory.getLog(ByteArrayInputStream.class);
    protected static final byte[]_endOfQueue=new byte[0];
    
    protected final Channel _inputQueue; // ByteBuffers are pushed onto here by producer, taken off by the consumer

    public ByteArrayInputStream(Channel inputQueue) {
        super();
        _inputQueue=inputQueue;
    }

    public int read() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

    // Puttable
    
    public void put(Object item) throws InterruptedException {
        //_log.info("putting buffer on input queue: "+item);
        _inputQueue.put(item);        
    }

    public boolean offer(Object item, long msecs) throws InterruptedException {
        return _inputQueue.offer(item, msecs);
    }
}
