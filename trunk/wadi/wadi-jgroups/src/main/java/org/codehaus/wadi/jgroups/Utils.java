package org.codehaus.wadi.jgroups;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

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
