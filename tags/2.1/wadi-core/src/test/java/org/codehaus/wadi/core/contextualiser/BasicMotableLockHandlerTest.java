package org.codehaus.wadi.core.contextualiser;

import java.util.concurrent.locks.Lock;

import org.codehaus.wadi.core.motable.Motable;

import com.agical.rmock.extension.junit.RMockTestCase;

public class BasicMotableLockHandlerTest extends RMockTestCase {

    private Invocation invocation;
    private Motable motable;
    private BasicMotableLockHandler lockHandler;

    @Override
    protected void setUp() throws Exception {
        invocation = (Invocation) mock(Invocation.class);
        motable = (Motable) mock(Motable.class);
        lockHandler = new BasicMotableLockHandler();
    }
    
    public void testAcquireWriteLock() throws Exception {
        invocation.isAcquireLockOnInvocationStart();
        modify().returnValue(true);
        invocation.isWithExclusiveLock();
        modify().returnValue(true);
        
        Lock lock = motable.getReadWriteLock().writeLock();
        lock.lockInterruptibly();

        startVerification();
        
        boolean acquired = lockHandler.acquire(invocation, motable);
        assertTrue(acquired);
    }
    
    public void testAcquireReadLock() throws Exception {
        invocation.isAcquireLockOnInvocationStart();
        modify().returnValue(true);
        invocation.isWithExclusiveLock();
        
        Lock lock = motable.getReadWriteLock().readLock();
        lock.lockInterruptibly();

        startVerification();
        
        boolean acquired = lockHandler.acquire(invocation, motable);
        assertTrue(acquired);
    }
    
    public void testAcquireDoNothing() throws Exception {
        invocation.isAcquireLockOnInvocationStart();
        
        startVerification();
        
        boolean acquired = lockHandler.acquire(invocation, motable);
        assertTrue(acquired);
    }
    
    public void testReleaseWriteLock() throws Exception {
        invocation.isReleaseLockOnInvocationEnd();
        modify().returnValue(true);
        invocation.isWithExclusiveLock();
        modify().returnValue(true);
        
        Lock lock = motable.getReadWriteLock().writeLock();
        lock.unlock();

        startVerification();
        
        lockHandler.release(invocation, motable);
    }
    
    public void testReleaseReadLock() throws Exception {
        invocation.isReleaseLockOnInvocationEnd();
        modify().returnValue(true);
        invocation.isWithExclusiveLock();
        
        Lock lock = motable.getReadWriteLock().readLock();
        lock.unlock();
        
        startVerification();
        
        lockHandler.release(invocation, motable);
    }
    
    public void testReleaseDoNothing() throws Exception {
        invocation.isReleaseLockOnInvocationEnd();
        
        startVerification();
        
        lockHandler.release(invocation, motable);
    }
    
}
