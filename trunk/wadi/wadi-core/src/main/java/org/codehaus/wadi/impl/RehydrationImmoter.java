/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.impl;

import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.motable.Motable;

/**
 * 
 * @version $Revision: 1538 $
 */
public class RehydrationImmoter implements Immoter {
    private final Immoter delegate;
    private final Motable emotable;
    
    public RehydrationImmoter(Immoter delegate, Motable emotable) {
        if (null == delegate) {
            throw new IllegalArgumentException("delegate is required");
        } else if (null == emotable) {
            throw new IllegalArgumentException("emotable is required");
        }
        this.delegate = delegate;
        this.emotable = emotable;
    }

    public boolean contextualise(Invocation invocation, String id, Motable immotable) throws InvocationException {
        return delegate.contextualise(invocation, id, immotable);
    }

    public boolean immote(Motable emotable, Motable immotable) {
        return delegate.immote(emotable, immotable);
    }

    public Motable newMotable() {
        Motable immotable = delegate.newMotable();
        try {
            immotable.rehydrate(emotable.getCreationTime(),
                    emotable.getLastAccessedTime(),
                    emotable.getMaxInactiveInterval(),
                    emotable.getName(),
                    emotable.getBodyAsByteArray());
        } catch (Exception e) {
            throw new WADIRuntimeException(e);
        }
        return immotable;
    }
}
