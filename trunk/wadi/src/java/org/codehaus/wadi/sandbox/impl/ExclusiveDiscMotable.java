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
package org.codehaus.wadi.sandbox.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.Motable;

/**
 * A Motable that represents its Bytes field as a File in a directory to which its node has Exclusive access.
 * N.B. The File field must be set before the Bytes field.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ExclusiveDiscMotable extends AbstractMotable {
	protected static final Log _log = LogFactory.getLog(ExclusiveDiscMotable.class);

	protected File _file;
	public File getFile() {return _file;}
	public void setFile(File file){_file=file;}

	protected byte[] _bytes;
	public byte[] getBytes(){return _bytes;}
	public void setBytes(byte[] bytes){_bytes=bytes;}

	public void tidy() {
	    super.tidy();
		if (_file!=null && _file.exists())
			remove(_file);
	}

	public void copy(Motable motable) throws Exception {
		super.copy(motable);
		store(_file, this);
	}

	protected static Motable load(File file, Motable motable) throws Exception {
		ObjectInputStream ois=null;
		try {
			ois=new ObjectInputStream(new FileInputStream(file));
			motable.setId((String)ois.readObject());
			motable.setCreationTime(ois.readLong());
			motable.setLastAccessedTime(ois.readLong());
			motable.setMaxInactiveInterval(ois.readInt());
			motable.setBytes((byte[])ois.readObject());

			if (!motable.checkTimeframe(System.currentTimeMillis()))
			    if (_log.isWarnEnabled()) _log.warn("loaded session from the future!: "+motable.getId());

			if (_log.isTraceEnabled()) _log.trace("loaded (exclusive disc): "+file);
			return motable;
		} catch (Exception e) {
			if (_log.isWarnEnabled()) _log.warn("load (exclusive disc) failed: "+file, e);
			throw e;
		}
		finally {
			if (ois!=null)
				ois.close();
		}
	}

	protected static void store(File file, Motable motable) throws Exception {
		ObjectOutputStream oos=null;
		try {
			oos=new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(motable.getId());
			oos.writeLong(motable.getCreationTime());
			oos.writeLong(motable.getLastAccessedTime());
			oos.writeInt(motable.getMaxInactiveInterval());
			oos.writeObject(motable.getBytes());
			oos.flush();
			if (_log.isTraceEnabled()) _log.trace("stored (exclusive disc): "+file);
		} catch (Exception e) {
			if (_log.isWarnEnabled()) _log.warn("store (exclusive disc) failed: "+file, e);
			throw e;
		} finally {
			if (oos!=null)
				oos.close();
		}
	}

	protected static void remove(File file) {
		file.delete();
		if (_log.isTraceEnabled()) _log.trace("removed (exclusive disc): "+file);
	}
}
