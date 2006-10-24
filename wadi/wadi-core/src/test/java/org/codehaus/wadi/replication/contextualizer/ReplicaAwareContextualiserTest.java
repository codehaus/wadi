package org.codehaus.wadi.replication.contextualizer;

import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.test.DummyDistributableSessionConfig;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.impl.DistributableSession;

import com.agical.rmock.extension.junit.RMockTestCase;

public class ReplicaAwareContextualiserTest extends RMockTestCase {

    private ReplicationManager manager;

    protected void setUp() throws Exception {
        manager = (ReplicationManager) mock(ReplicationManager.class);
    }
    
    public void testEmoter() throws Exception {
        ReplicaAwareContextualiser contextualiser = new ReplicaAwareContextualiser(null, manager);
        
        startVerification();
        
        Emoter emoter = contextualiser.getEmoter();
        
        WebSession emotable = new DistributableSession(new DummyDistributableSessionConfig());
        emotable.init(1, 2, 3);
        String attrKey = "attrKey";
        String attrValue = "attrValue";
        emotable.setAttribute(attrKey, attrValue);
        WebSession immotable = new DistributableSession(new DummyDistributableSessionConfig());
        emoter.prepare(null, emotable, immotable);
        
        assertEquals(emotable.getCreationTime(), immotable.getCreationTime());
        assertEquals(emotable.getLastAccessedTime(), immotable.getLastAccessedTime());
        assertEquals(emotable.getMaxInactiveInterval(), immotable.getMaxInactiveInterval());
        assertEquals(attrValue, immotable.getAttribute(attrKey));
    }

    public void testGet() throws Exception {
        WebSession motable = new DistributableSession(new DummyDistributableSessionConfig());
        String key = "id";
        
        manager.acquirePrimary(key);
        modify().returnValue(motable);
        
        startVerification();
        
        ReplicaAwareContextualiser contextualiser = new ReplicaAwareContextualiser(null, manager);
        Motable actualMotable = contextualiser.get(key);
        assertSame(motable, actualMotable);
    }
}
