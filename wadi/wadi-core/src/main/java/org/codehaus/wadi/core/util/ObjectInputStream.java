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
/**
 *
 */
package org.codehaus.wadi.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ObjectInputStream extends java.io.ObjectInputStream {

	protected final ClassLoader _classLoader;

	public ObjectInputStream(InputStream is, ClassLoader classLoader) throws IOException {
		super(is);
		_classLoader=classLoader;
	}

    private static final HashMap<String, Class<?>> PRIMITIVE_CLASSES =
        new HashMap<String, Class<?>>();

    static {
        PRIMITIVE_CLASSES.put("byte", byte.class);
        PRIMITIVE_CLASSES.put("short", short.class);
        PRIMITIVE_CLASSES.put("int", int.class);
        PRIMITIVE_CLASSES.put("long", long.class);
        PRIMITIVE_CLASSES.put("float", float.class);
        PRIMITIVE_CLASSES.put("double", double.class);
        PRIMITIVE_CLASSES.put("boolean", boolean.class);
        PRIMITIVE_CLASSES.put("char", char.class);
        PRIMITIVE_CLASSES.put("void", void.class);
    }

    protected Class resolveClass(ObjectStreamClass desc)
	throws IOException, ClassNotFoundException
	{
		String name = desc.getName();
		try {
			return Class.forName(name, false, _classLoader);
		} catch (ClassNotFoundException ex) {
			Class cl = (Class) PRIMITIVE_CLASSES.get(name);
			if (cl != null) {
				return cl;
			} else {
				throw ex;
			}
		}
	}

    protected Class resolveProxyClass(String[] interfaces)
	throws IOException, ClassNotFoundException
	{
		ClassLoader latestLoader = _classLoader;
		ClassLoader nonPublicLoader = null;
		boolean hasNonPublicInterface = false;

		// define proxy in class loader of non-public interface(s), if any
		Class[] classObjs = new Class[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			Class cl = Class.forName(interfaces[i], false, latestLoader);
			if ((cl.getModifiers() & Modifier.PUBLIC) == 0) {
				if (hasNonPublicInterface) {
					if (nonPublicLoader != cl.getClassLoader()) {
						throw new IllegalAccessError(
						"conflicting non-public interface class loaders");
					}
				} else {
					nonPublicLoader = cl.getClassLoader();
					hasNonPublicInterface = true;
				}
			}
			classObjs[i] = cl;
		}
		try {
			return Proxy.getProxyClass(
					hasNonPublicInterface ? nonPublicLoader : latestLoader,
							classObjs);
		} catch (IllegalArgumentException e) {
			throw new ClassNotFoundException(null, e);
		}
	}

}
