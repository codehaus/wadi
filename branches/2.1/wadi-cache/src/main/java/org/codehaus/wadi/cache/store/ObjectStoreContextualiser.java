/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.cache.store;

import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.core.contextualiser.AbstractSharedContextualiser;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.motable.AbstractChainedEmoter;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ObjectStoreContextualiser extends AbstractSharedContextualiser {
    private final ObjectLoader objectLoader;
    
    public ObjectStoreContextualiser(Contextualiser next, ObjectLoader objectLoader) {
        super(next);
        if (null == objectLoader) {
            throw new IllegalArgumentException("objectStore is required");
        }
        this.objectLoader = objectLoader;
    }

    @Override
    protected Motable get(String id, boolean exclusiveOnly) {
        ObjectInfoEntry objectInfoEntry;
        Object object = objectLoader.load(id);
        if (null == object) {
            objectInfoEntry = new ObjectInfoEntry(id, new ObjectInfo());
        } else {
            objectInfoEntry = new ObjectInfoEntry(id, new ObjectInfo(object));
        }
        return new ObjectMotable(objectInfoEntry);
    }

    @Override
    protected Emoter getEmoter() {
        return new AbstractChainedEmoter();
    }

    @Override
    protected Immoter getImmoter() {
        return new Immoter() {
            public boolean contextualise(Invocation invocation, String id, Motable immotable)
                    throws InvocationException {
                return false;
            }

            public boolean immote(Motable emotable, Motable immotable) {
                return false;
            }

            public Motable newMotable(Motable emotable) {
                return new ObjectMotable(null);
            }
        };
    }

}
