package org.codehaus.wadi.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.axiondb.jdbc.AxionDataSource;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.HttpServletRequestWrapperPool;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.AbstractExclusiveContextualiser;
import org.codehaus.wadi.impl.AlwaysEvicter;
import org.codehaus.wadi.impl.DatabaseMotable;
import org.codehaus.wadi.impl.DatabaseStore;
import org.codehaus.wadi.impl.DiscStore;
import org.codehaus.wadi.impl.DistributableAttributesFactory;
import org.codehaus.wadi.impl.DistributableManager;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyHttpServletRequest;
import org.codehaus.wadi.impl.DummyRouter;
import org.codehaus.wadi.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.impl.ExclusiveStoreContextualiser;
import org.codehaus.wadi.impl.GiannisContextualiser;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.NeverEvicter;
import org.codehaus.wadi.impl.ReplicableSession;
import org.codehaus.wadi.impl.ReplicableSessionFactory;
import org.codehaus.wadi.impl.SerialContextualiser;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SharedStoreContextualiser;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;
import org.codehaus.wadi.impl.Utils;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import junit.framework.TestCase;

public class TestReplication extends TestCase {

	protected Log _log = LogFactory.getLog(getClass());
	
	public TestReplication(String arg0) {
		super(arg0);
	}

    
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

    public void testReplication() throws Exception {

        int sweepInterval=1000*60*60*24; // 1 eviction/day
        boolean strictOrdering=true;
        Streamer streamer=new SimpleStreamer();
        Collapser collapser=new HashingCollapser(100, 1000);

        // Terminator
        Contextualiser terminator=new DummyContextualiser();
        
        // DB
        //DataSource ds=new AxionDataSource("jdbc:axiondb:WADI");
        MysqlDataSource msds=new MysqlDataSource();
        msds.setUrl("jdbc:mysql://localhost:3306/WADI?user=root");
        DataSource ds=msds;
        String table="TEST";
        DatabaseMotable.init(ds, table);
        Contextualiser db=new SharedStoreContextualiser(terminator, collapser, true, ds, "STORE");

        // Gianni
        Evicter devicter=new NeverEvicter(sweepInterval, strictOrdering);
        Map dmap=new HashMap();
        Contextualiser spool=new GiannisContextualiser(db, collapser, true, devicter, dmap, new DatabaseStore(ds, "SPOOL", false));
        
        Map mmap=new HashMap();
        
        Contextualiser serial=new SerialContextualiser(spool, collapser, mmap);

        SessionPool sessionPool=new SimpleSessionPool(new ReplicableSessionFactory());
        
        // Memory
        Evicter mevicter=new AlwaysEvicter(sweepInterval, strictOrdering);
        ContextPool contextPool=new SessionToContextPoolAdapter(sessionPool);
        HttpServletRequestWrapperPool requestPool=new DummyStatefulHttpServletRequestWrapperPool();
        AbstractExclusiveContextualiser memory=new MemoryContextualiser(serial, mevicter, mmap, streamer, contextPool, requestPool);

        // Manager
        AttributesFactory attributesFactory=new DistributableAttributesFactory();
        ValuePool valuePool=new SimpleValuePool(new DistributableValueFactory());
        SessionWrapperFactory wrapperFactory=new StandardSessionWrapperFactory();
        SessionIdFactory idFactory=new TomcatSessionIdFactory();
        DistributableManager manager=new DistributableManager(sessionPool, attributesFactory, valuePool, wrapperFactory, idFactory, memory, memory.getMap(), new DummyRouter(), streamer, true);
        manager.setSessionListeners(new HttpSessionListener[]{});
        manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
        manager.init(new DummyManagerConfig());

        manager.start();
        //mevicter.stop(); // we'll run it by hand...
        //devicter.stop();

        _log.info("CREATING SESSION");
        ReplicableSession session=(ReplicableSession)manager.create();
        String foo="bar";
        session.setAttribute("foo", foo);
        String name=session.getId();
        assertTrue(mmap.size()==1);
        assertTrue(dmap.size()==0);
        
        _log.info("TOUCHING SESSION");
        long lat=session.getLastAccessedTime();
        memory.contextualise(null, null, new FilterChain() { public void doFilter(ServletRequest req, ServletResponse res){_log.info("running request");} }, name, null, null, false);
        assert(lat!=session.getLastAccessedTime());
        session=(ReplicableSession)mmap.get(name);
        assertTrue(mmap.size()==1);
        assertTrue(dmap.size()==0);

        _log.info("DEMOTING SESSION to short-term SPOOL");
        mevicter.evict();
        assertTrue(mmap.size()==0);
        assertTrue(dmap.size()==1);
        
        _log.info("DEMOTING SESSION to long-term STORE");
        manager.stop();
        assertTrue(mmap.size()==0);
        assertTrue(dmap.size()==0);

        _log.info("PROMOTING SESSION to short-term SPOOL");
        manager.start();
        assertTrue(mmap.size()==0);
        assertTrue(dmap.size()==1);

        _log.info("PROMOTING SESSION to Memory");
        memory.contextualise(null, null, new FilterChain() { public void doFilter(ServletRequest req, ServletResponse res){_log.info("running request");} }, name, null, null, false);
        session=(ReplicableSession)mmap.get(name);
        assertTrue(session.getAttribute("foo")!=foo);
        assertTrue(session.getAttribute("foo").equals(foo));
        assertTrue(mmap.size()==1);
        assertTrue(dmap.size()==0);
        
        _log.info("DESTROYING SESSION");
        manager.destroy(session);
        assertTrue(mmap.size()==0);
        assertTrue(dmap.size()==0);
        
        manager.stop();

        DatabaseMotable.destroy(ds, table);
    }
}
