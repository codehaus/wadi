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

//import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//----------------------------------------

// TODO - consider scoping of Object identity and
// ObjectInput/OutputStreams... Do we need our own specialist OIS?

// TODO - EJB, Context and TX Wrappers need to go in here ...
// need other wrapper types for non-serialisable but distributable
// J2EE types... EJBs, XAs, Contexts etc...

// TODO - consider using specialist subtypes for
// HttpSessionActivationListener etc..

//----------------------------------------

/**
 * Each session attribute is wrapped with one of these objects. It
 * will manage [de]serialisation of the object and listener
 * notification.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
class Wrapper
  implements Serializable
{
  protected static transient final Log _log=LogFactory.getLog(Wrapper.class);

  protected transient AbstractHttpSessionImpl _ahsi;
  protected transient boolean                 _isActive;
  protected transient Object                  _activeValue;
  protected byte[]                            _passiveValue; // only this travels across the wire...

  public
    Wrapper(AbstractHttpSessionImpl ahsi, Object value)
  {
    _ahsi=ahsi;		// needed during passivation...
    _isActive=true;
    _activeValue=value;
  }

  public synchronized Object
    getValue(AbstractHttpSessionImpl ahsi)
  {
    if (!_isActive)		// lazy activation...
      activate(ahsi);

    return _activeValue;
  }

  public synchronized void
    setValue(Object value)
  {
    _passiveValue =null;
    _activeValue  =value;
    _isActive     =true;
  }

  protected void
    activate(AbstractHttpSessionImpl ahsi)
  {
    _ahsi=ahsi;		// if we have just come in across the wire we need to know our session

    if (_activeValue==null && _passiveValue!=null)
      _activeValue=ObjectInputStream.demarshall(_passiveValue);

    if (_log.isTraceEnabled()) _log.trace(_ahsi.getRealId()+" : activate: "+_passiveValue+" --> "+_activeValue);

    _passiveValue=null;		// no longer needed
    _isActive=true;		// ordered before notification to prevent reentry via getValue()

    if (_activeValue!=null && _activeValue instanceof HttpSessionActivationListener)
      ((HttpSessionActivationListener)_activeValue).sessionDidActivate(new HttpSessionEvent(_ahsi.getFacade()));
  }

  protected void
    passivate()
  {
    if (_activeValue!=null && _activeValue instanceof HttpSessionActivationListener)
      ((HttpSessionActivationListener)_activeValue).sessionWillPassivate(new HttpSessionEvent(_ahsi.getFacade()));

    _passiveValue=ObjectInputStream.marshall(_activeValue);
    _isActive=false;

    if (_log.isTraceEnabled()) _log.trace(_ahsi.getRealId()+" : passivate: "+_activeValue+" --> "+_passiveValue);
  }

  // standard serialisation API

  private synchronized void
    writeObject(ObjectOutputStream out)
      throws IOException
  {
    if (_isActive)
      passivate();

    out.defaultWriteObject();	// will only write out passivated value

    // we leave the object in passivated state - it will be
    // re-activated lazily...
  }

  private synchronized void
    readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException
  {
    in.defaultReadObject();	// will only read in passivated value
    // activation is done lazily
  }

  public String
    toString()
  {
    return ""+(_isActive?_activeValue:_passiveValue);
  }
}
