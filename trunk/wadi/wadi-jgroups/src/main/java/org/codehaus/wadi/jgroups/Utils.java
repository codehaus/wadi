/**
 *
 * Copyright ...
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
package org.codehaus.wadi.jgroups;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * A WADI Address that can be used to broadcast to a JGroups Channel
 * 
 * @version $Revision: 1647 $
 */
public class Utils {

    private Utils() {}

    public static String basename(Class clazz) {
        String name=clazz.getName();
        int i=name.lastIndexOf('.');
        return name.substring(i+1);
    }

    public static Object byteArrayToObject(byte[] state) throws IOException, ClassNotFoundException {
        ByteArrayInputStream memIn = new ByteArrayInputStream(state);
        ObjectInput oi = new ObjectInputStream(memIn);
        Object tmp = oi.readObject(); // TODO - ClassLoading ?
        oi.close();
        return tmp;
    }

    public static byte[] objectToByteArray(Object opaque) throws IOException {
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(memOut);
        oo.writeObject(opaque);
        oo.close();
        return memOut.toByteArray();
    }

}
