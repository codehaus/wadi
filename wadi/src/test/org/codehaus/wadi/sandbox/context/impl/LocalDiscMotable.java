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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A Motable that represents its Bytes field as a File on LocalDisc.
 * N.B. The File field must be set before the Bytes field.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class LocalDiscMotable extends AbstractMotable {
	protected static final Log _log = LogFactory.getLog(LocalDiscMotable.class);
	
	public LocalDiscMotable(){}
	
	protected File _file;
	//public File getFile() {return _file;}
	public void setFile(File file){_file=file;}
	
	public void tidy() {
		if (_file!=null && _file.exists())
			remove(_file);
	}
	
	// Motable
	public byte[] getBytes() throws IOException {return load(_file);}
	public void setBytes(byte[] bytes) throws IOException {store(_file, bytes);}
	
	protected static byte[] load(File file) throws IOException {
		int length=(int)file.length(); // length must be OK, or file would not exist
		FileInputStream fis=null;
		try {
			fis=new FileInputStream(file);
			byte[] buffer=new byte[length];
			fis.read(buffer, 0 ,length);
			_log.info("loaded (local disc): "+file);
			return buffer;
		} catch (IOException e) {
			_log.warn("load (local disc) failed: "+file, e);
			throw e;
		}
		finally {
			if (fis!=null)
				fis.close();
		}
	}
	
	protected static void store(File file, byte[] bytes) throws IOException {
		OutputStream os=null;
		try {
			os=new FileOutputStream(file);
			os.write(bytes);
			os.flush();
			_log.info("stored (local disc): "+file);
		} catch (IOException e) {
			_log.warn("store (local disc) failed: "+file, e);
			throw e;
		} finally {
			if (os!=null)
				os.close();			
		}
	}
	
	protected static void remove(File file) {
		file.delete();
		_log.info("removed (local disc): "+file);
	}
}