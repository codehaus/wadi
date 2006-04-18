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
package org.codehaus.wadi.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DistributableValueConfig;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class LazyValue extends DistributableValue {
    protected static final Log _log = LogFactory.getLog(LazyValue.class);

    protected transient boolean _listener;
    protected transient byte[] _bytes;

    public LazyValue(DistributableValueConfig config) {
        super(config);
    }

    protected void deserialise() {
        try {
            // deserialise content at last minute ...
            ByteArrayInputStream bais=new ByteArrayInputStream(_bytes);
            ObjectInputStream ois=new ObjectInputStream(bais);
            super.readContent(ois);
            ois.close();
        } catch (Exception e) {
	  _log.error("unexpected problem lazily deserialising session attribute value - data lost", e);
        } finally {
            _bytes=null;
        }
    }

    protected void serialise() throws IOException {
        ByteArrayOutputStream baos=new ByteArrayOutputStream(); // TODO - pool these objects...
        ObjectOutputStream oos=new ObjectOutputStream(baos);
        super.writeContent(oos);
        oos.close();
        _bytes=baos.toByteArray();
    }

    public synchronized Object getValue() {
        if (_bytes!=null)
            deserialise();

        return super.getValue();
    }

    public synchronized Object setValue(Object newValue) {
        if (_bytes!=null) {
            if (_listener || ((DistributableValueConfig)_config).getHttpSessionAttributeListenersRegistered())
                deserialise(); // oldValue needs deserialising before it is chucked...
        }

        Object tmp=super.setValue(newValue);
        _listener=(_value instanceof HttpSessionActivationListener) || (_value instanceof HttpSessionBindingListener); // doubles up on test in super...
        return tmp;
    }

    public synchronized void writeContent(ObjectOutput oo) throws IOException {
        if (_bytes==null)
            serialise(); // rebuild cache

        oo.writeBoolean(_listener);
        oo.writeInt(_bytes.length);
        oo.write(_bytes);
    }

    public synchronized void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        _listener=oi.readBoolean();
        int length=oi.readInt();
        _bytes=new byte[length];
        if (oi.read(_bytes)!=length)
            throw new IOException("data truncated whilst reading Session attribute value - data lost");
        _value=null;
    }

    public boolean isListener(){return _listener;}

    // should we register Listeners with our Attributes ?

}
