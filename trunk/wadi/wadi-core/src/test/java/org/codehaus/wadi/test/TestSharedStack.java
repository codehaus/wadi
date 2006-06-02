/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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

import java.util.HashMap;
import java.util.Map;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.PoolableInvocationWrapper;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.impl.AbstractSession;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyEvicter;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.SimpleStreamer;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import junit.framework.TestCase;

public class TestSharedStack extends TestCase {
    
    public TestSharedStack(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testSharedStack() throws Exception {
        assertTrue(true);
    }
    
    //---------------------------------------------------------------------
    // Tier1 components...
    
    static class Tier1Context extends AbstractSession {

        public void setBodyAsByteArray(byte[] bytes) throws Exception {
            throw new UnsupportedOperationException("NYI");
        }
        
    }
    
    static class Tier1ContextPool implements SessionPool {

        public void put(Session context) {
            // throw it away...
        }

        public Session take() {
           // create a new one...
            return new Tier1Context();
        }
        
    }

    static class Tier1Invocation extends DummyInvocation{
    }
    
    static class Tier1Wrapper implements PoolableInvocationWrapper {

        protected Invocation _invocation;
        protected Session _context;
        
        public void init(Invocation invocation, Session context) {
            _invocation=invocation;
            _context=context;
        }

        public void destroy() {
            _invocation=null;
            _context=null;
        }
        
    }
    
    static class Tier1WrapperPool implements PoolableInvocationWrapperPool {

        public PoolableInvocationWrapper take() {
            // create a new one...
            return new  Tier1Wrapper();
        }

        public void put(PoolableInvocationWrapper wrapper) {
            // throw it away...
        }
        
    }
    
    //---------------------------------------------------------------------

    public void testMemoryContextualiser() throws Exception {
        // server scope components...
        Contextualiser terminator=new DummyContextualiser();
        Evicter evicter=new DummyEvicter();
        Map map=new HashMap();
        
        // app specific components... - should be injected via the Invocation...
        Streamer streamer=new SimpleStreamer();
        SessionPool contextPool=new Tier1ContextPool();
        PoolableInvocationWrapperPool invocationWrapperPool=new Tier1WrapperPool(); // can't remember why we need this... - investigate
        
        Contextualiser memory=new MemoryContextualiser(terminator, evicter, map, streamer, contextPool, invocationWrapperPool);
        
        // add a session...
        String key="xxx";
        Session value=contextPool.take();
        map.put(key, value);
        
        // run an Invocation over it...
        Invocation invocation=new Tier1Invocation();
        Immoter immoter=null; // there is no-one above in the stack to promote to
        Sync motionLock=null;
        boolean exclusiveOnly=true;
        memory.contextualise(invocation, key, immoter, motionLock, exclusiveOnly);
    }
    
}
