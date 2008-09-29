/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.core.motable;

import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.MotableLockHandler;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class LockingRehydrationImmoterTest extends RMockTestCase {

    private Immoter delegate;
    private LockingRehydrationImmoter immoter;
    private Invocation invocation;
    private MotableLockHandler lockHandler;

    @Override
    protected void setUp() throws Exception {
        delegate = (Immoter) mock(Immoter.class);
        invocation = (Invocation) mock(Invocation.class);
        lockHandler = (MotableLockHandler) mock(MotableLockHandler.class);
        immoter = new LockingRehydrationImmoter(delegate, invocation, lockHandler);
    }
    
    public void testMotableCannotBeLockedReturnFalse() throws Exception {
        Motable immotable = (Motable) mock(Motable.class);

        lockHandler.acquire(invocation, immotable);
        modify().returnValue(false);
        
        startVerification();
        
        boolean immoted = immoter.immote(null, immotable);
        assertFalse(immoted);
    }
    
    public void testMotableIsLockedPriorToImmote() throws Exception {
        Motable immotable = (Motable) mock(Motable.class);
        Motable emotable = (Motable) mock(Motable.class);
        
        lockHandler.acquire(invocation, immotable);
        modify().returnValue(true);
        
        delegate.immote(emotable, immotable);
        modify().returnValue(true);
        
        startVerification();
        
        boolean immoted = immoter.immote(emotable, immotable);
        assertTrue(immoted);
    }
    
    public void testMotableIsUnLockedAfterContextualization() throws Exception {
        Motable immotable = (Motable) mock(Motable.class);

        beginSection(s.ordered("Contextualize and unlock"));
        delegate.contextualise(invocation, null, immotable);
        modify().returnValue(true);
        
        lockHandler.release(invocation, immotable);
        endSection();

        startVerification();
        
        boolean contextualised = immoter.contextualise(invocation, null, immotable);
        assertTrue(contextualised);
    }
    
}
