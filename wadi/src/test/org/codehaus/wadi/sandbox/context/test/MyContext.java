/**
*
* Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.SimpleStreamingStrategy;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.impl.AbstractMotable;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;


public class MyContext extends AbstractMotable implements Context {
	protected static Log _log = LogFactory.getLog(MyContext.class);
	
	protected String _val;
	protected transient ReadWriteLock _lock;

	MyContext(String val) {
		this();
		_id=val;
		_val=val;
	}

	MyContext() {
		_lock=new ReaderPreferenceReadWriteLock();
		}

	public Sync getSharedLock(){return _lock.readLock();}
	public Sync getExclusiveLock(){return _lock.writeLock();}

	// SerializableContext...

	public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
		_val=(String)oi.readObject();
	}

	public void writeContent(ObjectOutput oo) throws IOException, ClassNotFoundException {
		oo.writeObject(_val);
	}
	
	// Motable
	public byte[] getBytes() throws Exception {
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		ObjectOutputStream oos=null;
		try {
			oos=new ObjectOutputStream(baos);
			writeContent(oos);
			return baos.toByteArray();
		} catch (Exception e) {
			_log.warn("problem serialising context to byte[]", e);
			throw e;
		} finally {
			if (oos!=null)
				oos.close();
		}
	}
	
	public void setBytes(byte[] bytes) throws IOException, ClassNotFoundException {
		readContent(new SimpleStreamingStrategy().getInputStream(new ByteArrayInputStream(bytes)));
	}
	
	public void tidy() {
		_val=null;
	}
}