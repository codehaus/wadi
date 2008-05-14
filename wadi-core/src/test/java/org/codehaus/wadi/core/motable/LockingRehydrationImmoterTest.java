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

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class LockingRehydrationImmoterTest extends RMockTestCase {

    private Immoter delegate;
    private LockingRehydrationImmoter immoter;

    @Override
    protected void setUp() throws Exception {
        delegate = (Immoter) mock(Immoter.class);
        immoter = new LockingRehydrationImmoter(delegate);
    }
    
    public void testMotableIsLockedPriorToImmote() throws Exception {
        Motable emotable = (Motable) mock(Motable.class);
        Motable immotable = (Motable) mock(Motable.class);

        beginSection(s.ordered("Lock and immote"));
        immotable.getReadWriteLock().readLock().lockInterruptibly();
        delegate.immote(emotable, immotable);
        endSection();
        startVerification();
        
        immoter.immote(emotable, immotable);
    }
    
    public void testMotableIsUnLockedAfterContextualization() throws Exception {
        Motable immotable = (Motable) mock(Motable.class);

        beginSection(s.ordered("Contextualize and unlock"));
        delegate.contextualise(null, null, immotable);
        immotable.getReadWriteLock().readLock().unlock();
        endSection();
        startVerification();
        
        immoter.contextualise(null, null, immotable);
    }
    
}
