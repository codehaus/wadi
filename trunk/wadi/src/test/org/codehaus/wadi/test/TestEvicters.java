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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;


import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.EvicterConfig;
import org.codehaus.wadi.HttpServletRequestWrapperPool;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.AbsoluteEvicter;
import org.codehaus.wadi.impl.AbstractExclusiveContextualiser;
import org.codehaus.wadi.impl.DistributableAttributesFactory;
import org.codehaus.wadi.impl.DistributableManager;
import org.codehaus.wadi.impl.DistributableSessionFactory;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyRouter;
import org.codehaus.wadi.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.impl.ExclusiveStoreContextualiser;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.NeverEvicter;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.impl.jetty.JettySessionWrapperFactory;
import org.codehaus.wadi.io.impl.DummyServer;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import junit.framework.TestCase;

public class TestEvicters extends TestCase {

    public TestEvicters(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public static class MyEvicterConfig implements EvicterConfig {

        protected final Timer _timer=new Timer();
        public Timer getTimer() {return _timer;}

        protected final Map _map=new HashMap();
        public Map getMap() {return _map;}

        protected final Sync _sync=new NullSync();
        public Sync getEvictionLock(String id, Motable motable) {return _sync;}

        protected final Emoter _emoter=new EtherEmoter();
        public Emoter getEvictionEmoter() {return _emoter;}

        protected final int _maxInactiveInterval=4;
        public int getMaxInactiveInterval() {return _maxInactiveInterval;}

        protected int _demotions;
        public int getDemotions() {return _demotions;}
        public void demote(Motable motable) {_demotions++;}

        protected int _expirations;
        public int getExpirations() {return _expirations;}
        public void expire(Motable motable) {_expirations++; _map.remove(motable.getName());}
    }

    public void testExpiryFromStorage() throws Exception {
        // Contextualiser
        Contextualiser next=new DummyContextualiser();
        int sweepInterval=1;
        boolean strictOrdering=true;
        Evicter devicter=new NeverEvicter(sweepInterval, strictOrdering);
        Map dmap=new HashMap();
        Streamer streamer=new SimpleStreamer();
        // (Contextualiser next, Collapser collapser, Evicter evicter, Map map, StreamingStrategy streamer, File dir) {
        Collapser collapser=new HashingCollapser(100, 1000);
        File dir=new File("/tmp/wadi/"+System.getProperty("wadi.colour"));
        Contextualiser disc=new ExclusiveStoreContextualiser(next, collapser, true, devicter, dmap, streamer, dir);
        Map mmap=new HashMap();
        int inactivityInterval=1; // second
        Evicter mevicter=new AbsoluteEvicter(sweepInterval, strictOrdering, inactivityInterval);
        SessionPool sessionPool=new SimpleSessionPool(new DistributableSessionFactory());
        ContextPool contextPool=new SessionToContextPoolAdapter(sessionPool);
        HttpServletRequestWrapperPool requestPool=new DummyStatefulHttpServletRequestWrapperPool();
        AbstractExclusiveContextualiser memory=new MemoryContextualiser(disc, mevicter, mmap, streamer, contextPool, requestPool);
        // Manager
        AttributesFactory attributesFactory=new DistributableAttributesFactory();
        ValuePool valuePool=new SimpleValuePool(new DistributableValueFactory());
        SessionWrapperFactory wrapperFactory=new JettySessionWrapperFactory();
        SessionIdFactory idFactory=new TomcatSessionIdFactory();
        DistributableManager manager=new DistributableManager(sessionPool, attributesFactory, valuePool, wrapperFactory, idFactory, memory, memory.getMap(), new DummyRouter(), streamer, true, null, new DummyServer());
        manager.setMaxInactiveInterval(2);
        manager.init();
        //manager.start();
        //mevicter.stop(); // we'll run it by hand...
        //devicter.stop();

        manager.create();
        assertTrue(mmap.size()==1);
        assertTrue(dmap.size()==0);
        Thread.sleep(1100);
        mevicter.evict();
        assertTrue(mmap.size()==0);
        assertTrue(dmap.size()==1);
        Thread.sleep(1100);
        devicter.evict();
        assertTrue(mmap.size()==0);
        assertTrue(dmap.size()==0);
        manager.stop();

        // rename/use IdGenerator and StreamingStrategy...

    }
}
