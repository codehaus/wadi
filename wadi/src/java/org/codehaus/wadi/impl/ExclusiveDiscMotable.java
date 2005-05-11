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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ExclusiveDiscMotableConfig;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.StoreMotable;
import org.codehaus.wadi.StoreMotableConfig;

/**
 * A Motable that represents its Bytes field as a File in a directory to which its node has Exclusive access.
 * N.B. The File field must be set before the Bytes field.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ExclusiveDiscMotable extends AbstractMotable implements StoreMotable {
	protected static final Log _log = LogFactory.getLog(ExclusiveDiscMotable.class);

    protected ExclusiveDiscMotableConfig _config;
    protected File _file;
    
    public void init(StoreMotableConfig config) { // used when we are going to store something...
        _config=(ExclusiveDiscMotableConfig)config;
    }
 
    public void init(StoreMotableConfig config, String name) throws Exception { // used when we are going to load something...
        _config=(ExclusiveDiscMotableConfig)config;
        _file=new File(_config.getDirectory(), name+_config.getSuffix());
        load(_file, this);
        assert(name.equals(_name));
    }

	protected byte[] _bytes;
	public byte[] getBytes(){return _bytes;}
	public void setBytes(byte[] bytes) throws Exception {_bytes=bytes;}

    public void copy(Motable motable) throws Exception {
        super.copy(motable);
        _file=new File(_config.getDirectory(), _name+_config.getSuffix());
        store(_file, this);
    }
    
	public void destroy() { // causes a 'remove'
	    super.destroy();
		if (_file!=null && _file.exists())
			remove(_file);
	}

	protected static void load(File file, ExclusiveDiscMotable motable) throws Exception {
		ObjectInputStream ois=null;
		try {
			ois=new ObjectInputStream(new FileInputStream(file));

			motable._creationTime=ois.readLong();
            motable._lastAccessedTime=ois.readLong();
            motable._maxInactiveInterval=ois.readInt();
            motable._name=(String)ois.readObject();
			motable._bytes=(byte[])ois.readObject();

			if (!motable.checkTimeframe(System.currentTimeMillis()))
			    if (_log.isWarnEnabled()) _log.warn("loaded (exclusive disc) from the future!: "+motable.getName());

			if (_log.isTraceEnabled()) _log.trace("loaded (exclusive disc): "+file);
		} catch (Exception e) {
			if (_log.isWarnEnabled()) _log.warn("load (exclusive disc) failed: "+file, e);
			throw e;
		}
		finally {
			if (ois!=null)
				ois.close();
		}
	}

	protected static void store(File file, ExclusiveDiscMotable motable) throws Exception {
		ObjectOutputStream oos=null;
		try {
			oos=new ObjectOutputStream(new FileOutputStream(file));

			oos.writeLong(motable._creationTime);
			oos.writeLong(motable._lastAccessedTime);
			oos.writeInt(motable._maxInactiveInterval);
            oos.writeObject(motable._name);
			oos.writeObject(motable._bytes);
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
