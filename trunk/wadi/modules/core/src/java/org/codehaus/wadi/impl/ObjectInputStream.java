/**
 * 
 */
package org.codehaus.wadi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;

public class ObjectInputStream extends java.io.ObjectInputStream {
	
	protected final ClassLoader _classLoader;
	
	public ObjectInputStream(InputStream is, ClassLoader classLoader) throws IOException {
		super(is);
		_classLoader=classLoader;
	}
	
	// copied from super as this seems to be the only way to parameterise the ClassLoader... - TODO
	
	/*
	 * @(#)ObjectInputStream.java	1.146 04/01/13
	 *
	 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
	 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
	 */
	private static final HashMap primClasses = new HashMap(8, 1.0F);
	static {
		primClasses.put("boolean", boolean.class);
		primClasses.put("byte", byte.class);
		primClasses.put("char", char.class);
		primClasses.put("short", short.class);
		primClasses.put("int", int.class);
		primClasses.put("long", long.class);
		primClasses.put("float", float.class);
		primClasses.put("double", double.class);
		primClasses.put("void", void.class);
	}
	
    protected Class resolveClass(ObjectStreamClass desc)
	throws IOException, ClassNotFoundException
	{
		String name = desc.getName();
		try {
			return Class.forName(name, false, _classLoader);
		} catch (ClassNotFoundException ex) {
			Class cl = (Class) primClasses.get(name);
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