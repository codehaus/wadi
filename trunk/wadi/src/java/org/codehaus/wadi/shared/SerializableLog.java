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

package org.codehaus.wadi.shared;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SerializableLog
    implements Serializable
  {
    protected transient Log _log;
    protected String _name;

    public SerializableLog(Class clazz) {_name=clazz.getName(); init();}
    public SerializableLog(String name) {_name=name;init();}

    protected void init(){_log=LogFactory.getLog(_name);}

    private void readObject(java.io.ObjectInputStream ois)
      throws java.io.IOException, ClassNotFoundException
    {
      try
      {
	ois.defaultReadObject();
	init();
      }
      catch (Exception e)
      {
	System.err.println("aaarrrgh!");
      }
    }

    public boolean isInfoEnabled(){return _log.isInfoEnabled();}
    public boolean isDebugEnabled(){return _log.isDebugEnabled();}
    public boolean isTraceEnabled(){return _log.isTraceEnabled();}

    public void info(String info){_log.info(info);}
    public void warn(String warn){_log.warn(warn);}
    public void warn(String warn, Exception e){_log.warn(warn, e);}
    public void trace(String trace){_log.trace(trace);}
  }