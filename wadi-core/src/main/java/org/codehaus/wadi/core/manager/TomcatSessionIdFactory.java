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

// TODO - how do I license this file ?

/*
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */

// this class has been put together from code taken from Tomcat5
// org.apache.catalina.session.ManagerBase. When I have the time, I
// will invetigate UID generation and write my own generator...

// How hard can it be to generate a secure session id ? :-)

package org.codehaus.wadi.core.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.Random;

/**
 * An IdGenerator borrowed from Tomcat
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TomcatSessionIdFactory
  implements SessionIdFactory
  {
    protected final Log _log = LogFactory.getLog(getClass());

    public String create() {
        String id=generateSessionId();
        if (log.isTraceEnabled()) log.trace("generated: "+id);
        return id;
    }

    public int getSessionIdLength() {
        return 32;
    }

    public void setSessionIdLength(int l) {
        if (l!=getSessionIdLength())
            if (_log.isWarnEnabled()) _log.warn("session id length is not a writeable attribute - ignoring new setting: " + l);
    }

//   // we should be able to better than this - can't we work out the
//   // size of the ID ?
//   public String
//     getId(String idId, String idRoute)
//   {
//     return idId+"."+idRoute;
//   }

//   public String
//     getIdId(String id)
//     {
//       int index=id.indexOf('.');
//       return index<0?id:id.substring(0,index);
//     }

//   public String
//     getIdRoute(String id)
//     {
//       int index=id.indexOf('.');
//       return index<0?null:id.substring(index+1,id.length());
//     }

  //------------------------------------------------
  // integration layer..
  //------------------------------------------------

  protected final Log log=LogFactory.getLog(getClass());

  class StringManager
  {
    String getString(String s){return s;}
    String getString(String s, String a){return "["+a+"]: "+s;}
  }

  protected StringManager sm=new StringManager();

  class Support
  {
    void firePropertyChange(String s, Object oldVal, Object newVal){}	// TODO - do we really need this ?
  }

  protected Support support=new Support();

  //------------------------------------------------
  // everything below this line borrowed from Tomcat
  //------------------------------------------------

  protected DataInputStream randomIS=null;
  protected String devRandomSource="/dev/urandom";

  /**
   * The default message digest algorithm to use if we cannot use
   * the requested one.
   */
  protected static final String DEFAULT_ALGORITHM = "MD5";

  /**
   * The number of random bytes to include when generating a
   * session identifier.
   */
  protected static final int SESSION_ID_BYTES = 16;

  /**
   * The message digest algorithm to be used when generating session
   * identifiers.  This must be an algorithm supported by the
   * <code>java.security.MessageDigest</code> class on your platform.
   */
  protected String algorithm = DEFAULT_ALGORITHM;

  /**
   * Return the MessageDigest implementation to be used when
   * creating session identifiers.
   */
  protected MessageDigest digest = null;

  /**
   * A random number generator to use when generating session identifiers.
   */
  protected Random random = null;

  /**
   * The Java class name of the random number generator class to be used
   * when generating session identifiers.
   */
  protected String randomClass = "java.security.SecureRandom";

  /**
   * A String initialization parameter used to increase the entropy of
   * the initialization of our random number generator.
   */
  protected String entropy = null;

  /**
   * Return the entropy increaser value, or compute a semi-useful value
   * if this String has not yet been set.
   */
  public String getEntropy() {

    // Calculate a semi-useful value if this has not been set
    if (this.entropy == null)
      setEntropy(this.toString());

    return (this.entropy);

  }

  /**
   * Set the entropy increaser value.
   *
   * @param entropy The new entropy increaser value
   */
  public void setEntropy(String entropy) {

    String oldEntropy = entropy;
    this.entropy = entropy;
    support.firePropertyChange("entropy", oldEntropy, this.entropy);

  }

  /**
   * Return the random number generator instance we should use for
   * generating session identifiers.  If there is no such generator
   * currently defined, construct and seed a new one.
   */
  public synchronized Random getRandom() {
    if (this.random == null) {
      synchronized (this) {
	if (this.random == null) {
	  // Calculate the new random number generator seed
	  long seed = System.currentTimeMillis();
	  //long t1 = seed;
	  char entropy[] = getEntropy().toCharArray();
	  for (int i = 0; i < entropy.length; i++) {
	    long update = ((byte) entropy[i]) << ((i % 8) * 8);
	    seed ^= update;
	  }
	  try {
	    // Construct and seed a new random number generator
	    Class clazz = Class.forName(randomClass);
	    this.random = (Random) clazz.newInstance();
	    this.random.setSeed(seed);
	  } catch (Exception e) {
	    // Fall back to the simple case
            log.error(sm.getString("managerBase.random", randomClass), e);
	    this.random = new java.util.Random();
	    this.random.setSeed(seed);
	  }
//	  long t2=System.currentTimeMillis();
//	  if( (t2-t1) > 100 )
//	    if (log.isTraceEnabled())
//	      log.trace(sm.getString("managerBase.seeding", randomClass) + " " + (t2-t1));
	}
      }
    }

    return (this.random);

  }

  private class PrivilegedSetRandomFile implements PrivilegedAction{

    public Object run(){
      try {
	File f=new File( devRandomSource );
	if( ! f.exists() ) return null;
	randomIS= new DataInputStream( new FileInputStream(f));
	randomIS.readLong();
//	if( log.isTraceEnabled() )
//	  log.trace( "Opening " + devRandomSource );
	return randomIS;
      } catch (IOException ex){
	return null;
      }
    }
  }

  /** Use /dev/random-type special device. This is new code, but may reduce the
   *  big delay in generating the random.
   *
   *  You must specify a path to a random generator file. Use /dev/urandom
   *  for linux ( or similar ) systems. Use /dev/random for maximum security
   *  ( it may block if not enough "random" exist ). You can also use
   *  a pipe that generates random.
   *
   *  The code will check if the file exists, and default to java Random
   *  if not found. There is a significant performance difference, very
   *  visible on the first call to getSession ( like in the first JSP )
   *  - so use it if available.
   */
  public void setRandomFile( String s ) {
    // as a hack, you can use a static file - and genarate the same
    // session ids ( good for strange traceging )
    if (System.getSecurityManager() != null){
      randomIS = (DataInputStream)AccessController.doPrivileged(new PrivilegedSetRandomFile());
    } else {
      try{
	devRandomSource=s;
	File f=new File( devRandomSource );
	if( ! f.exists() ) return;
	randomIS= new DataInputStream( new FileInputStream(f));
	randomIS.readLong();
//	if( log.isTraceEnabled() )
//	  log.trace( "Opening " + devRandomSource );
      } catch( IOException ex ) {
	randomIS=null;
      }
    }
  }

  /**
   * Generate and return a new session identifier.
   */
  protected synchronized String generateSessionId() {
    byte bytes[] = new byte[SESSION_ID_BYTES];
    getRandomBytes( bytes );
    bytes = getDigest().digest(bytes);

    // Render the result as a String of hexadecimal digits
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < bytes.length; i++) {
      byte b1 = (byte) ((bytes[i] & 0xf0) >> 4);
      byte b2 = (byte) (bytes[i] & 0x0f);
      if (b1 < 10)
	result.append((char) ('0' + b1));
      else
	result.append((char) ('A' + (b1 - 10)));
      if (b2 < 10)
	result.append((char) ('0' + b2));
      else
	result.append((char) ('A' + (b2 - 10)));
    }
    return (result.toString());

  }

  /**
   * Return the MessageDigest object to be used for calculating
   * session identifiers.  If none has been created yet, initialize
   * one the first time this method is called.
   */
  public synchronized MessageDigest getDigest() {

    if (this.digest == null) {
      //long t1=System.currentTimeMillis();
//      if (log.isTraceEnabled())
//	log.trace(sm.getString("managerBase.getting", algorithm));
      try {
	this.digest = MessageDigest.getInstance(algorithm);
      } catch (NoSuchAlgorithmException e) {
	log.error(sm.getString("managerBase.digest", algorithm), e);
	try {
	  this.digest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
	} catch (NoSuchAlgorithmException f) {
	  log.error(sm.getString("managerBase.digest", DEFAULT_ALGORITHM), e);
	  this.digest = null;
	}
      }
//      if (log.isTraceEnabled())
//	log.trace(sm.getString("managerBase.gotten"));
//      long t2=System.currentTimeMillis();
//      if( log.isTraceEnabled() )
//	log.trace("getDigest() " + (t2-t1));
    }

    return (this.digest);

  }

  protected void getRandomBytes( byte bytes[] ) {
    // Generate a byte array containing a session identifier
    if( devRandomSource!=null && randomIS==null ) {
      setRandomFile( devRandomSource );
    }
    if(randomIS!=null ) {
      try {
	int len=randomIS.read( bytes );
	if( len==bytes.length ) {
	  return;
	}
//	if (log.isTraceEnabled())
//	  log.trace("Got " + len + " " + bytes.length );
      } catch( Exception ex ) {
      }
      devRandomSource=null;
      randomIS=null;
    }
//    Random random = getRandom();
    getRandom().nextBytes(bytes);
  }
}
