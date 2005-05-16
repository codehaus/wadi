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

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Puttable;
import EDU.oswego.cs.dl.util.concurrent.Takable;

public class Utils {

    protected static Log _log=LogFactory.getLog(Utils.class);
    
    public static void safePut(Object item, Puttable puttable) {
        do {
            try {
                puttable.put(item);
            } catch (InterruptedException e) {
                if (_log.isTraceEnabled()) _log.trace("unexpected interruption - ignoring", e);
            }
        } while (Thread.interrupted());
    }

    public static Object safeTake(Takable takable) {
        do {
            try {
                return (Object)takable.take();
            } catch (InterruptedException e) {
                if (_log.isTraceEnabled()) _log.trace("unexpected interruption - ignoring", e);
            }
        } while (Thread.interrupted());
        
        throw new IllegalStateException();
    }

}
