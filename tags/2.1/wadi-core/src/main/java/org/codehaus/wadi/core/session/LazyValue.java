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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class LazyValue extends DistributableValue {
    protected static final Log _log = LogFactory.getLog(LazyValue.class);

    protected transient byte[] bytes;

    public LazyValue(ValueHelperRegistry valueHelperRegistry) {
        super(valueHelperRegistry);
    }

    protected void deserialise() {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            super.readExternal(ois);
            ois.close();
        } catch (Exception e) {
            _log.error("unexpected problem lazily deserialising session attribute value - data lost", e);
        } finally {
            bytes = null;
        }
    }

    protected byte[] serialise() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        super.writeExternal(oos);
        oos.close();
        return baos.toByteArray();
    }

    public synchronized Object getValue() {
        if (bytes != null) {
            deserialise();
        }
        return super.getValue();
    }

    public synchronized Object setValue(Object newValue) {
        if (bytes != null) {
            deserialise();
        }
        return super.setValue(newValue);
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        bytes = serialise();
        oo.writeInt(bytes.length);
        oo.write(bytes);
    }

    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        int length = oi.readInt();
        bytes = new byte[length];
        if (oi.read(bytes) != length) {
            throw new IOException("data truncated whilst reading Session attribute value - data lost");
        }
    }

}
