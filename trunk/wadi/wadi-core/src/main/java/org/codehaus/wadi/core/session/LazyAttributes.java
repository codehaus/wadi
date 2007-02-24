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
package org.codehaus.wadi.core.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ValueFactory;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class LazyAttributes extends DistributableAttributes {
    protected static final Log _log = LogFactory.getLog(LazyAttributes.class);

    protected transient byte[] _bytes;

    public LazyAttributes(ValueFactory valueFactory) {
        super(valueFactory);
    }

    protected void deserialise() {
        try {
            // deserialise content at last minute ...
            ByteArrayInputStream bais = new ByteArrayInputStream(_bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            super.readExternal(ois);
            ois.close();
        } catch (Exception e) {
            _log.error("unexpected problem lazily deserialising session attribute value - data lost", e);
        } finally {
            _bytes = null;
        }
    }

    protected void serialise() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); // TODO -
                                                                    // pool
                                                                    // these
                                                                    // objects...
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        super.writeExternal(oos);
        oos.close();
        _bytes = baos.toByteArray();
    }

    public synchronized Object get(Object key) {
        if (_bytes != null)
            deserialise();

        return super.get(key);
    }

    public Object remove(Object key) {
        if (_bytes != null)
            deserialise();
        return super.remove(key);
    }

    public Object put(Object key, Object newValue) {
        if (_bytes != null)
            deserialise();
        return super.put(key, newValue);
    }

    public int size() {
        if (_bytes != null)
            deserialise();
        return super.size();
    }

    public Set keySet() {
        if (_bytes != null)
            deserialise();
        return super.keySet();
    }

    public void clear() {
        _bytes = null;
    }

    public Set getListenerNames() {
        if (_bytes != null)
            deserialise();
        return _listenerNames;
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        if (_bytes == null) {
            serialise(); // rebuild cache
        }

        oo.writeInt(_bytes.length);
        oo.write(_bytes);
    }

    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        int length = oi.readInt();
        _bytes = new byte[length];
        if (oi.read(_bytes) != length)
            throw new IOException("data truncated whilst reading Session Attributes- data lost");
        attributes.clear();
    }

}
