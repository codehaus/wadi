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

package org.codehaus.wadi.test;

import java.io.ObjectStreamException;
import java.io.Serializable;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.shared.ObjectInputStream;

/**
 * Serialisation related tests
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
class Shared
  implements Serializable
{
  protected transient Log _log=LogFactory.getLog(getClass());

  public Shared(){}
  public Shared(Shared s){_payload=s._payload;}
  protected int _payload;
  public void setPayload(int t){_payload=t;}
  public int getPayload(){return _payload;}

  public String toString(){return "<"+getClass().getName()+":"+_payload+">";}

  protected Object
    writeReplace()
    throws ObjectStreamException
  {
    _log.info("writing porter");
    Porter p=new Porter(this);
    _log.info(""+p);
    return p;
  }
}

class Porter
  extends Shared
  implements Serializable
{

  public static Class _target;
  public static java.lang.reflect.Constructor _ctor;

  static void
    setUp(Class target)
    throws NoSuchMethodException
  {
    _target=target;
    _ctor=_target.getConstructor(new Class[]{Shared.class});
  }

  Porter(Shared s){super(s);}

  protected Object
    readResolve()
    throws ObjectStreamException
  {
    _log=LogFactory.getLog(getClass());	// why do transient fields not get properly initialised ?
    _log.info(""+_payload);
    try
    {
      return _ctor.newInstance(new Shared[]{this});
    }
    catch (Exception any)
    {
      throw new ObjectStreamException(){};
    }
  }

  protected Object
    writeReplace()
    throws ObjectStreamException
  {
    return this;		// need to override super to prevent double replacement
  }
}

class Tomcat
  extends Shared
  implements Serializable
{
  public Tomcat() {_payload=20;}
  public Tomcat(Shared s) {super(s);}
}

class Jetty
  extends Shared
  implements Serializable
{
  public Jetty() {_payload=10;}
  public Jetty(Shared s) {super(s);}
}



public class
  TestSerialisation
  extends TestCase
{
  protected Log _log=LogFactory.getLog(TestHttpSession.class);

  public TestSerialisation(String name) {super(name);}

  protected void
    setUp()
    throws Exception
    {
      _log.info("starting test");
    }

  protected void
    tearDown()
    throws InterruptedException
    {
      _log.info("stopping test");
    }

  public void
    testRoundTrip()
    throws Exception
    {
      byte[] buffer;
      Object o1;
      Object o2;

      Tomcat tc=new Tomcat();
      tc.setPayload(100);

      o1=tc;
      _log.info("outbound instance is: "+o1);
      buffer=ObjectInputStream.marshall(o1);
      Porter.setUp(Jetty.class);
      o2=ObjectInputStream.demarshall(buffer);
      _log.info("inbound instance is: "+o2);

      _log.info("outbound instance is: "+o2);
      buffer=ObjectInputStream.marshall(o2);
      Porter.setUp(Tomcat.class);
      o1=ObjectInputStream.demarshall(buffer);
      _log.info("inbound instance is: "+o1);
    }
}
