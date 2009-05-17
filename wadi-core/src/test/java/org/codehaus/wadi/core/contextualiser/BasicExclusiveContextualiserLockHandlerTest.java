package org.codehaus.wadi.core.contextualiser;

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.motable.Motable;

import com.agical.rmock.extension.junit.RMockTestCase;

public class BasicExclusiveContextualiserLockHandlerTest extends RMockTestCase {

    private ConcurrentMotableMap map;
    private BasicExclusiveContextualiserLockHandler lockHandler;
    private Invocation invocation;
    private String id;

    @Override
    protected void setUp() throws Exception {
        id = "id";
        invocation = (Invocation) mock(Invocation.class);
        map = (ConcurrentMotableMap) mock(ConcurrentMotableMap.class);
        lockHandler = new BasicExclusiveContextualiserLockHandler(map);
    }

    public void testAcquireWriteLock() throws Exception {
        invocation.isAcquireLockOnInvocationStart();
        modify().returnValue(true);
        invocation.isWithExclusiveLock();
        modify().returnValue(true);
        
        invocation.getExclusiveSessionLockWaitTime();
        int exclusiveSessionLockWaitTime = 100;
        modify().returnValue(exclusiveSessionLockWaitTime);
        
        Motable motable = map.acquireExclusive(id, exclusiveSessionLockWaitTime);
        
        startVerification();
        
        Motable acquired = lockHandler.acquire(invocation, id);
        assertSame(motable, acquired);
    }
    
    public void testAcquireReadLock() throws Exception {
        invocation.isAcquireLockOnInvocationStart();
        modify().returnValue(true);
        invocation.isWithExclusiveLock();
        
        Motable motable = map.acquire(id);
        
        startVerification();
        
        Motable acquired = lockHandler.acquire(invocation, id);
        assertSame(motable, acquired);
    }
    
    public void testAcquireGetWithoutLock() throws Exception {
        invocation.isAcquireLockOnInvocationStart();
        
        Motable motable = map.get(id);
        
        startVerification();
        
        Motable acquired = lockHandler.acquire(invocation, id);
        assertSame(motable, acquired);
    }
    
    public void testReleaseWriteLock() throws Exception {
        Motable motable = (Motable) mock(Motable.class);
        
        invocation.isReleaseLockOnInvocationEnd();
        modify().returnValue(true);
        invocation.isWithExclusiveLock();
        modify().returnValue(true);
        
        map.releaseExclusive(motable);
        
        startVerification();
        
        lockHandler.release(invocation, motable);
    }
    
    public void testReleaseReadLock() throws Exception {
        Motable motable = (Motable) mock(Motable.class);
        
        invocation.isReleaseLockOnInvocationEnd();
        modify().returnValue(true);
        invocation.isWithExclusiveLock();
        
        map.release(motable);
        
        startVerification();
        
        lockHandler.release(invocation, motable);
    }
    
    public void testReleaseDoNothing() throws Exception {
        Motable motable = (Motable) mock(Motable.class);
        
        invocation.isReleaseLockOnInvocationEnd();
        
        startVerification();
        
        lockHandler.release(invocation, motable);
    }
    
}
