package org.codehaus.wadi.core.eviction;

import java.util.Collections;

import org.codehaus.wadi.EvictionStrategy;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.eviction.AbstractBestEffortEvicter;

import com.agical.rmock.extension.junit.RMockTestCase;

public class AbstractBestEffortEvicterTest extends RMockTestCase {

    private Motable motable;
    private ConcurrentMotableMap idToEvictable;
    private EvictionStrategy evictionStrategy;

    protected void setUp() throws Exception {
        motable = (Motable) mock(Motable.class);
        idToEvictable = (ConcurrentMotableMap) mock(ConcurrentMotableMap.class);
        evictionStrategy = (EvictionStrategy) mock(EvictionStrategy.class);
    }
    
    public void testExpiration() throws Exception {
        AbstractBestEffortEvicter evicter = new AbstractBestEffortEvicter(10, false) {
            public boolean testForDemotion(Motable motable, long time, long ttl) {
                throw new UnsupportedOperationException();
            }
        };

        beginSection(s.ordered("Expire"));
        idToEvictable.getNames();
        String id = "id";
        modify().returnValue(Collections.singleton(id));

        idToEvictable.acquireExclusive(id);
        modify().returnValue(motable);

        motable.getTimeToLive(10);
        modify().args(is.ANYTHING).returnValue(-1);
        idToEvictable.remove(id);
        
        idToEvictable.releaseExclusive(motable);

        evictionStrategy.expire(motable);
        endSection();
        startVerification();

        evicter.evict(idToEvictable, evictionStrategy);
    }

    public void testDemotion() throws Exception {
        AbstractBestEffortEvicter evicter = new AbstractBestEffortEvicter(10, false) {
            public boolean testForDemotion(Motable motable, long time, long ttl) {
                return true;
            }
        };

        beginSection(s.ordered("Demote"));
        idToEvictable.getNames();
        String id = "id";
        modify().returnValue(Collections.singleton(id));

        idToEvictable.acquireExclusive(id);
        modify().returnValue(motable);

        motable.getTimeToLive(10);
        modify().multiplicity(expect.exactly(2)).args(is.ANYTHING).returnValue(10);
        idToEvictable.remove(id);
        
        idToEvictable.releaseExclusive(motable);

        evictionStrategy.demote(motable);
        endSection();
        startVerification();

        evicter.evict(idToEvictable, evictionStrategy);
    }

    public void testNothingToDo() throws Exception {
        AbstractBestEffortEvicter evicter = new AbstractBestEffortEvicter(10, false) {
            public boolean testForDemotion(Motable motable, long time, long ttl) {
                return false;
            }
        };

        beginSection(s.ordered("Do nothing"));
        idToEvictable.getNames();
        String id = "id";
        modify().returnValue(Collections.singleton(id));

        idToEvictable.acquireExclusive(id);
        modify().returnValue(motable);

        motable.getTimeToLive(10);
        modify().multiplicity(expect.exactly(2)).args(is.ANYTHING).returnValue(10);
        
        idToEvictable.releaseExclusive(motable);

        endSection();
        startVerification();

        evicter.evict(idToEvictable, evictionStrategy);
    }

}
