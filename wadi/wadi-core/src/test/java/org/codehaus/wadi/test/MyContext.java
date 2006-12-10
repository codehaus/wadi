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
package org.codehaus.wadi.test;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.impl.AbstractSession;
import org.codehaus.wadi.impl.SimpleStreamer;
import org.codehaus.wadi.impl.Utils;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MyContext extends AbstractSession {
    protected String val;
    protected Streamer streamer = new SimpleStreamer();

    public MyContext(String id, String val) {
        name = id;
        this.val = val;
    }

    public MyContext() {
    }

    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        name = oi.readUTF();
        val = oi.readUTF();
    }

    public void writeContent(ObjectOutput oo) throws IOException {
        oo.writeUTF(name);
        oo.writeUTF(val);
    }

    public void setBodyAsByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        Utils.setContent(this, bytes, streamer);
    }

}
