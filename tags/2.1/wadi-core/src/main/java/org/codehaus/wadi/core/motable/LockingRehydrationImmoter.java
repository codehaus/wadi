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
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.contextualiser.MotableLockHandler;

/**
 * 
 * @version $Revision: 1538 $
 */
public class LockingRehydrationImmoter extends RehydrationImmoter {
    
    private final Invocation invocation;
    private final MotableLockHandler lockHandler;

    public LockingRehydrationImmoter(Immoter delegate, Invocation invocation, MotableLockHandler lockHandler) {
        super(delegate);
        if (null == invocation) {
            throw new IllegalArgumentException("invocation is required");
        } else if (null == lockHandler) {
            throw new IllegalArgumentException("lockHandler is required");
        }
        this.invocation = invocation;
        this.lockHandler = lockHandler;
    }

    public boolean contextualise(Invocation invocation, String id, Motable immotable) throws InvocationException {
        try {
            return super.contextualise(invocation, id, immotable);
        } finally {
            lockHandler.release(invocation, immotable);
        }
    }

    public boolean immote(Motable emotable, Motable immotable) {
        if (!lockHandler.acquire(invocation, immotable)) {
            return false;
        }
        
        return super.immote(emotable, immotable);
    }

}
