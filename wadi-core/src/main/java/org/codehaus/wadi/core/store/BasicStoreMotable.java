/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.core.store;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.codehaus.wadi.core.motable.AbstractMotable;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 2267 $
 */
public class BasicStoreMotable extends AbstractMotable {
    private final Store store;
    private byte[] body;
    
    public BasicStoreMotable(Store store) {
        if (null == store) {
            throw new IllegalArgumentException("store is required");
        }
        this.store = store;
    }
    
    public byte[] getBodyAsByteArray() throws Exception {
        if (null == body) {
            body = store.loadBody(this); 
        }
        return body;
    }

    public void setBodyAsByteArray(byte[] body) throws Exception {
        this.body = body;
        store.insert(this);
    }

    public void destroy() throws Exception {
        store.delete(this);
        super.destroy();
    }

    public void destroyForMotion() throws Exception {
        store.delete(this);
        super.destroyForMotion();
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        throw new UnsupportedOperationException();
    }

    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException();
    }
    
}
