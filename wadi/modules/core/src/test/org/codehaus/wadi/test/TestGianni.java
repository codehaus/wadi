package org.codehaus.wadi.test;

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
import org.codehaus.wadi.impl.DatabaseReplicater;
import org.codehaus.wadi.impl.DatabaseStore;
import org.codehaus.wadi.impl.DistributableAttributesFactory;
import org.codehaus.wadi.impl.DistributableManager;
import org.codehaus.wadi.impl.DistributableValueFactory;
import org.codehaus.wadi.impl.DummyContextualiser;
import org.codehaus.wadi.impl.DummyRouter;
import org.codehaus.wadi.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.impl.GiannisContextualiser;
import org.codehaus.wadi.impl.HashingCollapser;
import org.codehaus.wadi.impl.MemoryContextualiser;
import org.codehaus.wadi.impl.NeverEvicter;
import org.codehaus.wadi.impl.AbstractReplicableSession;
import org.codehaus.wadi.impl.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.impl.SerialContextualiser;
import org.codehaus.wadi.impl.SessionToContextPoolAdapter;
import org.codehaus.wadi.impl.SimpleSessionPool;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.SimpleValuePool;
import org.codehaus.wadi.impl.StandardSessionWrapperFactory;
import org.codehaus.wadi.impl.TomcatSessionIdFactory;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import junit.framework.TestCase;

public class TestGianni extends TestCase {

  protected Log _log = LogFactory.getLog(getClass());

  public TestGianni(String arg0) {
    super(arg0);
  }

  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void TtestGianni() throws Exception {

	    int sweepInterval=1000*60*60*24; // 1 eviction/day
	    boolean strictOrdering=true;
	    Streamer streamer=new SimpleStreamer();
	    Collapser collapser=new HashingCollapser(100, 1000);

	    // Terminator
	    Contextualiser terminator=new DummyContextualiser();

	    // DB
	    //String url="jdbc:axiondb:WADI";
	    //DataSource ds=new AxionDataSource(url);
	    MysqlDataSource msds=new MysqlDataSource();
	    String url="jdbc:mysql://localhost:3306/WADI";
	    msds.setUrl(url+"?user=root");
	    msds.setLoggerClassName(MySqlLogger.class.getName());
	    msds.setProfileSQL(true);
	    DataSource ds=msds;
	    String storeTable="SESSIONS";
	    DatabaseStore store=new DatabaseStore(url, ds, storeTable, false, true, true);

	    // Gianni
	    Evicter devicter=new NeverEvicter(sweepInterval, strictOrdering);
	    Map dmap=new HashMap();
	    boolean clean=true;
	    Contextualiser db=new GiannisContextualiser(terminator, collapser, clean, devicter, dmap, store);

	    Map mmap=new HashMap();

	    Contextualiser serial=new SerialContextualiser(db, collapser, mmap);

	    SessionPool sessionPool=new SimpleSessionPool(new AtomicallyReplicableSessionFactory());

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
	    DistributableManager manager=new DistributableManager(sessionPool, attributesFactory, valuePool, wrapperFactory, idFactory, memory, memory.getMap(), new DummyRouter(), streamer, true, new DatabaseReplicater(store, true));
	    manager.setSessionListeners(new HttpSessionListener[]{});
	    manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
	    manager.init(new DummyManagerConfig());

	    manager.start();
	    //mevicter.stop(); // we'll run it by hand...
	    //devicter.stop();

	    _log.info("CREATING SESSION");
	    AbstractReplicableSession session=(AbstractReplicableSession)manager.create();
	    String foo="bar";
	    session.setAttribute("foo", foo);
	    String name=session.getId();
	    assertTrue(mmap.size()==1);
	    assertTrue(dmap.size()==0);

	    _log.info("TOUCHING SESSION");
	    long lat=session.getLastAccessedTime();
	    memory.contextualise(null, null, new FilterChain() { public void doFilter(ServletRequest req, ServletResponse res){_log.info("running request");} }, name, null, null, false);
	    assert(lat!=session.getLastAccessedTime());
	    session=(AbstractReplicableSession)mmap.get(name);
	    assertTrue(mmap.size()==1);
	    assertTrue(dmap.size()==0);

//	    _log.info("DEMOTING SESSION to short-term SPOOL");
//	    mevicter.evict();
//	    assertTrue(mmap.size()==0);
//	    assertTrue(dmap.size()==1);

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
	    session=(AbstractReplicableSession)mmap.get(name);
	    assertTrue(session.getAttribute("foo")!=foo);
	    assertTrue(session.getAttribute("foo").equals(foo));
	    assertTrue(mmap.size()==1);
	    assertTrue(dmap.size()==0);

	    _log.info("DESTROYING SESSION");
	    manager.destroy(session);
	    assertTrue(mmap.size()==0);
	    assertTrue(dmap.size()==0);

	    manager.stop();

	    store.destroy();
	  }
  
  public void testTimeOut() throws Exception {

	    int sweepInterval=1;
	    boolean strictOrdering=true;
	    Streamer streamer=new SimpleStreamer();
	    Collapser collapser=new HashingCollapser(100, 1000);

	    // Terminator
	    Contextualiser terminator=new DummyContextualiser();

	    // DB
	    //String url="jdbc:axiondb:WADI";
	    //DataSource ds=new AxionDataSource(url);
	    MysqlDataSource msds=new MysqlDataSource();
	    String url="jdbc:mysql://localhost:3306/WADI";
	    msds.setUrl(url+"?user=root");
	    msds.setLoggerClassName(MySqlLogger.class.getName());
	    msds.setProfileSQL(true);
	    DataSource ds=msds;
	    String storeTable="SESSIONS";
	    DatabaseStore store=new DatabaseStore(url, ds, storeTable, false, true, true);

	    // Gianni
	    Evicter devicter=new NeverEvicter(sweepInterval, strictOrdering);
	    Map dmap=new HashMap();
	    boolean clean=true;
	    Contextualiser db=new GiannisContextualiser(terminator, collapser, clean, devicter, dmap, store);

	    Map mmap=new HashMap();

	    Contextualiser serial=new SerialContextualiser(db, collapser, mmap);

	    SessionPool sessionPool=new SimpleSessionPool(new AtomicallyReplicableSessionFactory());

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
	    DistributableManager manager=new DistributableManager(sessionPool, attributesFactory, valuePool, wrapperFactory, idFactory, memory, memory.getMap(), new DummyRouter(), streamer, true, new DatabaseReplicater(store, true));
	    manager.setSessionListeners(new HttpSessionListener[]{});
	    manager.setAttributelisteners(new HttpSessionAttributeListener[]{});
	    manager.setMaxInactiveInterval(3);
	    manager.init(new DummyManagerConfig());

	    manager.start();

	    _log.info("CREATING SESSION");
	    AbstractReplicableSession session=(AbstractReplicableSession)manager.create();
	    String foo="bar";
	    session.setAttribute("foo", foo);
	    String name=session.getId();
	    assertTrue(mmap.size()==1);
	    assertTrue(dmap.size()==0);

	    _log.info("DEMOTING SESSION to long-term STORE");
	    Thread.sleep(2000);
	    assertTrue(mmap.size()==0);
	    assertTrue(dmap.size()==1);

	    _log.info("TIMING SESSION OUT");
	    Thread.sleep(3000);
	    assertTrue(mmap.size()==0);
	    assertTrue(dmap.size()==0);

	    store.destroy();
	  }

}
