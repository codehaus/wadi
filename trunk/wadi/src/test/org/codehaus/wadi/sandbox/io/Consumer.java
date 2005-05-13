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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Takable;

public class Consumer implements Runnable {

    protected final static Log _log=LogFactory.getLog(Consumer.class);
    
    protected final Takable _input;
    protected final Thread _thread;
    
    public Consumer(Takable input) {
        super();
        _input=input;
        _thread=new Thread(this);
    }

    protected boolean _running;
    
    public void start() {
        _running=true;
        _thread.start();
    }
    
    public void stop() {
        _running=false;
        _thread.interrupt();
    }
    
    public void run() {
        do {
            _log.info("waiting for connection");
            try {
                Runnable connection=(Runnable)_input.take();
                _log.info("got a connection");
                connection.run();
                _log.info("processed connection");
            } catch (InterruptedException e) {
                _log.warn(e);
            }
        } while (Thread.interrupted() || _running);
        
        _log.info("leaving service");
    }

}
