package org.codehaus.wadi.core.eviction;

import java.util.Collections;

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.MotableBusyException;
import org.codehaus.wadi.core.contextualiser.EvictionStrategy;
import org.codehaus.wadi.core.motable.Motable;

import com.agical.rmock.extension.junit.RMockTestCase;

public class TestAbstractBestEffortEvicterTest extends RMockTestCase {

    private Motable motable;
    private ConcurrentMotableMap idToEvictable;
    private EvictionStrategy evictionStrategy;

    protected void setUp() throws Exception {
        motable = (Motable) mock(Motable.class);
        idToEvictable = (ConcurrentMotableMap) mock(ConcurrentMotableMap.class);
        evictionStrategy = (EvictionStrategy) mock(EvictionStrategy.class);
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

        idToEvictable.acquireExclusive(id, 1);
        modify().returnValue(motable);

        motable.getTimeToLive(10);
        modify().args(is.ANYTHING).returnValue(10);
        idToEvictable.remove(id);
        
        idToEvictable.releaseExclusive(motable);

        evictionStrategy.demote(motable);
        endSection();
        startVerification();

        evicter.evict(idToEvictable, evictionStrategy);
    }

    public void testBuzyMotablesAreSkipped() throws Exception {
        AbstractBestEffortEvicter evicter = new AbstractBestEffortEvicter(10, false) {
            public boolean testForDemotion(Motable motable, long time, long ttl) {
                throw new UnsupportedOperationException();
            }
        };

        beginSection(s.ordered("Expire"));
        idToEvictable.getNames();
        String id = "id";
        modify().returnValue(Collections.singleton(id));

        idToEvictable.acquireExclusive(id, 1);
        modify().throwException(new MotableBusyException("buzy"));

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

        idToEvictable.acquireExclusive(id, 1);
        modify().returnValue(motable);

        motable.getTimeToLive(10);
        modify().args(is.ANYTHING).returnValue(10);
        
        idToEvictable.releaseExclusive(motable);

        endSection();
        startVerification();

        evicter.evict(idToEvictable, evictionStrategy);
    }

}
