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

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO - is this really the correct way to do this ? - why isn't
// SecurityManager an interface ?

// TODO - we will use this to figure out when application-space
// threads are created/terminate. In order to be able to guarantee
// threadsafe access to unsynchronized aplication-space resources, we
// need to know when there are application-space threads running that
// might access said resources. We should also try to actively enforce
// the spec, particularly where it say that app child threads should
// not outlive their parent's time within the container...

/**
 * Not in use. The intention was to monitor applications use of custom threads.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  SecurityManager
  extends java.lang.SecurityManager
{
  protected final Log _log=LogFactory.getLog(getClass());
  protected final java.lang.SecurityManager _delegate;

  public SecurityManager(java.lang.SecurityManager delegate){_delegate=delegate;}
  public java.lang.SecurityManager getDelegate(){return _delegate;}

  // java.lang.SecurityManager...
  public Object getSecurityContext(){return _delegate==null?null:_delegate.getSecurityContext();}
  public ThreadGroup getThreadGroup(){return _delegate==null?null:_delegate.getThreadGroup();}
  public boolean getInCheck(){return _delegate==null?false:_delegate.getInCheck();}
  public boolean checkTopLevelWindow(Object window){return _delegate==null?false:_delegate.checkTopLevelWindow(window);}
  public void checkAccept(String host, int port){if (_delegate!=null) _delegate.checkAccept(host, port);}
  public void checkAwtEventQueueAccess(){if (_delegate!=null) _delegate.checkAwtEventQueueAccess();}
  public void checkConnect(String host, int port){if (_delegate!=null) _delegate.checkConnect(host, port);}
  public void checkConnect(String host, int port, Object context){if (_delegate!=null) _delegate.checkConnect(host, port, context);}
  public void checkDelete(String file){if (_delegate!=null) _delegate.checkDelete(file);}
  public void checkExec(String cmd){if (_delegate!=null) _delegate.checkExec(cmd);}
  public void checkExit(int status){if (_delegate!=null) _delegate.checkExit(status);}
  public void checkLink(String lib){if (_delegate!=null) _delegate.checkLink(lib);}
  public void checkListen(int port){if (_delegate!=null) _delegate.checkListen(port);}
  public void checkMemberAccess(Class clazz, int which){if (_delegate!=null) _delegate.checkMemberAccess(clazz, which);}
  public void checkMulticast(InetAddress maddr){if (_delegate!=null) _delegate.checkMulticast(maddr);}
  public void checkMulticast(InetAddress maddr, byte ttl){if (_delegate!=null) _delegate.checkMulticast(maddr, ttl);}
  public void checkPackageAccess(String pkg){if (_delegate!=null) _delegate.checkPackageAccess(pkg);}
  public void checkPackageDefinition(String pkg){if (_delegate!=null) _delegate.checkPackageDefinition(pkg);}
  public void checkPermission(Permission perm){if (_delegate!=null) _delegate.checkPermission(perm);}
  public void checkPermission(Permission perm, Object context){if (_delegate!=null) _delegate.checkPermission(perm, context);}
  public void checkPrintJobAccess(){if (_delegate!=null) _delegate.checkPrintJobAccess();}
  public void checkPropertiesAccess(){if (_delegate!=null) _delegate.checkPropertiesAccess();}
  public void checkPropertyAccess(String key){if (_delegate!=null) _delegate.checkPropertyAccess(key);}
  public void checkRead(FileDescriptor fd){if (_delegate!=null) _delegate.checkRead(fd);}
  public void checkRead(String file){if (_delegate!=null) _delegate.checkRead(file);}
  public void checkRead(String file, Object context){if (_delegate!=null) _delegate.checkRead(file, context);}
  public void checkSecurityAccess(String target){if (_delegate!=null) _delegate.checkSecurityAccess(target);}
  public void checkSetFactory(){if (_delegate!=null) _delegate.checkSetFactory();}
  public void checkSystemClipboardAccess(){if (_delegate!=null) _delegate.checkSystemClipboardAccess();}
  public void checkWrite(FileDescriptor fd){if (_delegate!=null) _delegate.checkWrite(fd);}
  public void checkWrite(String file){if (_delegate!=null) _delegate.checkWrite(file);}

  public void
    checkAccess(Thread t)
    {
      _log.info("checkAccess(Thread "+t+") called");
      if (_delegate!=null) _delegate.checkAccess(t);
    }

  public void
    checkAccess(ThreadGroup g)
    {
      _log.info("checkAccess(ThreadGroup "+g+") called");
      if (_delegate!=null) _delegate.checkAccess(g);
    }
}
