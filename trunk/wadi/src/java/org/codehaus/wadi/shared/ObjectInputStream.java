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

// TODO - do we really need this?

package org.codehaus.wadi.shared;

import java.io.IOException;
//import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// based on stream of the same name from J2SDK - do we really need
// this now ?

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ObjectInputStream
  extends java.io.ObjectInputStream
{
  private static final Log _log=LogFactory.getLog(ObjectInputStream.class);

  private static final Map _primClasses = new HashMap(8, 1.0F);
  static
  {
    _primClasses.put("boolean" , boolean.class);
    _primClasses.put("byte"    , byte.class);
    _primClasses.put("char"    , char.class);
    _primClasses.put("short"   , short.class);
    _primClasses.put("int"     , int.class);
    _primClasses.put("long"    , long.class);
    _primClasses.put("float"   , float.class);
    _primClasses.put("double"  , double.class);
    _primClasses.put("void"    , void.class);
  }


  ObjectInputStream(InputStream is)
    throws IOException
  {
    super(is);
  }

  // is this really necessary ?
  protected Class
    resolveClass(java.io.ObjectStreamClass desc)
    throws IOException, ClassNotFoundException
  {
    String name = desc.getName();
    try
    {
      return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
    }
    catch (ClassNotFoundException ex)
    {
      Class cl = (Class) _primClasses.get(name);
      if (cl != null)
	return cl;
      else
	throw ex;
    }
  }

  public static byte[]
    marshall(Object value)
  {
    try
    {
      if (value==null)
	return null;

      ByteArrayOutputStream baos=new ByteArrayOutputStream();
      ObjectOutputStream    oos =new ObjectOutputStream(baos);
      oos.writeObject(value);
      oos.flush();
      return baos.toByteArray();
    }
    catch (IOException ioe)
    {
      _log.warn("problem marshalling attribute", ioe);
      return null;
    }
  }

  public static Object
    demarshall(byte[] buffer)
  {
    try
    {
      if (buffer==null)
	return buffer;

      ByteArrayInputStream bais=new ByteArrayInputStream(buffer);
      ObjectInputStream    ois =new ObjectInputStream(bais);
      return ois.readObject();
    }
    catch (Exception e)
    {
      _log.warn("problem demarshalling attribute", e);
      return null;
    }
  }
}

