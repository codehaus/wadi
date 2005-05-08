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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Motable;

/**
 * Implement all of Motable except for the Bytes field. This is the field most likely to have different representations.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractMotable extends SimpleEvictable implements Motable, Serializable {
 
    protected static Log _log = LogFactory.getLog(AbstractMotable.class);

    public void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name) {
        init(creationTime, lastAccessedTime, maxInactiveInterval);
        _name=name;
    }

    public void destroy() {
        super.destroy();
        _name=null;
        // should setBytes(null)... - TODO - although init() does not initialise them...
    }

	public void copy(Motable motable) throws Exception {
		super.copy(motable); // Evictable fields
		_name=motable.getName();
		setBytes(motable.getBytes());
	}

	protected String _name;
	public String getName(){return _name;}

	// N.B. implementation of Bytes field is left abstract...

    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        super.readContent(oi);
        _name=(String)oi.readObject();
//        int length=oi.readInt();
//        byte[] bytes=new byte[length];
//        int actualLength=oi.read(bytes);
//        if (actualLength!=length)
//            if (_log.isErrorEnabled()) _log.error("serialized session truncated - "+(length-actualLength)+" bytes lost");
//        try {
//            setBytes(bytes);
//        } catch (Exception e) {
//            _log.error("unexpected problem deserializing session - data lost", e);
//        }
    }

    public void writeContent(ObjectOutput oo) throws IOException {
        super.writeContent(oo);
        oo.writeObject(_name);
//        try {
//            byte[] bytes=getBytes();
//            oo.writeInt(bytes.length);
//            oo.write(bytes);
//        } catch (Exception e) {
//            _log.error("unexpected problem serializing session - data lost", e);
//        }
    }
}


