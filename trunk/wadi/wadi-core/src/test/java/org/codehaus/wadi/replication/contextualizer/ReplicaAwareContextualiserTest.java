package org.codehaus.wadi.replication.contextualizer;

import junit.framework.TestCase;

import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.impl.DistributableSession;
import org.codehaus.wadi.replication.manager.BaseMockReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.test.DummyDistributableSessionConfig;

public class ReplicaAwareContextualiserTest extends TestCase {

    public void testEmoter() throws Exception {
        ReplicaAwareContextualiser contextualiser = new ReplicaAwareContextualiser(null, null);
        Emoter emoter = contextualiser.getEmoter();
        
        Session emotable = new DistributableSession(new DummyDistributableSessionConfig());
        emotable.init(1, 2, 3);
        String attrKey = "attrKey";
        String attrValue = "attrValue";
        emotable.setAttribute(attrKey, attrValue);
        Session immotable = new DistributableSession(new DummyDistributableSessionConfig());
        emoter.prepare(null, emotable, immotable);
        
        assertEquals(emotable.getCreationTime(), immotable.getCreationTime());
        assertEquals(emotable.getLastAccessedTime(), immotable.getLastAccessedTime());
        assertEquals(emotable.getMaxInactiveInterval(), immotable.getMaxInactiveInterval());
        assertEquals(attrValue, immotable.getAttribute(attrKey));
    }

    public void testGet() {
        final Session motable = new DistributableSession(new DummyDistributableSessionConfig());
        final Object[] parameters = new Object[1];
        ReplicationManager manager = new BaseMockReplicationManager() {
            public Object acquirePrimary(Object key) {
                parameters[0] = key;
                return motable;
            }
        };
        
        ReplicaAwareContextualiser contextualiser = new ReplicaAwareContextualiser(null, manager);
        String key = "id";
        Motable actualMotable = contextualiser.get(key);
        assertSame(key, parameters[0]);
        assertSame(motable, actualMotable);
    }
}
