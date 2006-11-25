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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.Streamer;

import EDU.oswego.cs.dl.util.concurrent.Puttable;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.Takable;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * A collection of useful static functions
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class Utils {
	protected static final Log _log=LogFactory.getLog(Utils.class);
	protected static final Log _lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");

	/**
	 * Mote (in other words - move) the data held in a Motable from one Contextualiser to another, such
	 * that if the two Contextualisers store Motables in a persistant fashion, the data is never
	 * present in less than one of the two.
	 *
	 * @param emoter - delegate for the source Contextualiser
	 * @param immoter - delegate for the target Contextualiser
	 * @param emotable - data to be moved
	 * @param name - the id of said data
	 * @return - the resulting immotable - in other words - the data's new representation in the target Contextualiser
	 */
	public static Motable mote(Emoter emoter, Immoter immoter, Motable emotable, String name) {
        Motable immotable = immoter.newMotable();
        boolean immotionOK = immoter.immote(emotable, immotable);
        if (!immotionOK) {
            return null;
        }
        emoter.emote(emotable, immotable);
        return immotable;
    }

    public static void acquireUninterrupted(String lockType, String lockName, Sync sync) throws TimeoutException {
        boolean interrupted = false;
        do {
            try {
                if (_lockLog.isTraceEnabled()) {
                    _lockLog.trace(lockType + " - acquiring: " + lockName + " [" + Thread.currentThread().getName()
                            + "]" + " : " + sync);
                }
                sync.acquire();
                if (_lockLog.isTraceEnabled()) {
                    _lockLog.trace(lockType + " - acquired : " + lockName + " [" + Thread.currentThread().getName()
                            + "]" + " : " + sync);
                }
            } catch (TimeoutException e) {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                throw e;
            } catch (InterruptedException e) {
                _log.trace("unexpected interruption - ignoring", e);
                interrupted = true;
            }
        } while (Thread.interrupted());
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
    
	public static void release(String lockType, String lockName, Sync sync) {
		if (_lockLog.isTraceEnabled()) {
            _lockLog.trace(lockType + " - releasing: " + lockName + " [" + Thread.currentThread().getName() + "]"
                    + " : " + sync);
        }
        sync.release();
        if (_lockLog.isTraceEnabled()) {
            _lockLog.trace(lockType + " - released : " + lockName + " [" + Thread.currentThread().getName() + "]"
                    + " : " + sync);
        }
	}

	/**
	 * If a lock is free, acquire it, ignoring any interruptions, else fail.
	 *
	 * @param sync - the lock
	 * @return - whether or not the lock was acquired
	 */
	public static boolean attemptUninterrupted(String lockType, String lockName, Sync sync) {
        boolean interrupted = false;
	    boolean acquired = false;
        do {
            try {
                if (_lockLog.isTraceEnabled()) {
                    _lockLog.trace(lockType + " - acquiring: " + lockName + " [" + Thread.currentThread().getName()
                            + "]" + " : " + sync);
                }
                acquired = sync.attempt(0);
                if (_lockLog.isTraceEnabled()) {
                    _lockLog.trace(lockType + " - acquired : " + lockName + " [" + Thread.currentThread().getName()
                            + "]" + " : " + sync);
                }
            } catch (InterruptedException e) {
                _log.trace("unexpected interruption - ignoring", e);
                interrupted = true;
            }
        } while (Thread.interrupted());

        if (!acquired) {
            if (_lockLog.isTraceEnabled()) {
                _lockLog.trace(lockType + " - acquisition FAILED: " + lockName + " ["
                        + Thread.currentThread().getName() + "]" + " : " + sync);
            }
        }
        
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return acquired;
	}

	public static Object byteArrayToObject(byte[] bytes, Streamer streamer) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream bais=new ByteArrayInputStream(bytes);
	    ObjectInput oi=streamer.getInputStream(bais);
	    Object tmp=oi.readObject(); // TODO - ClassLoading ?
	    oi.close();
	    return tmp;
	}

	public static Object safeByteArrayToObject(byte[] bytes, Streamer streamer) {
	    try {
	        return byteArrayToObject(bytes, streamer);
	    } catch (Exception e) {
	      _log.error("unexpected problem whilst unmarshalling", e);
	        return null;
	    }
	}

	public static byte[] objectToByteArray(Object object, Streamer streamer) throws IOException {
	    ByteArrayOutputStream baos=new ByteArrayOutputStream();
	    ObjectOutput oo=streamer.getOutputStream(baos);
	    oo.writeObject(object);
	    oo.close();
	    return baos.toByteArray();
	}

    public static byte[] safeObjectToByteArray(Object object, Streamer streamer) {
        try {
            return objectToByteArray(object, streamer);
        } catch (Exception e) {
	  _log.error("unexpected problem whilst marshalling", e);
            return null;
        }
    }

    public static byte[] getContent(SerializableContent object, Streamer streamer) throws IOException {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ObjectOutput oo=streamer.getOutputStream(baos);
        object.writeContent(oo);
        oo.close();
        return baos.toByteArray();
    }

    public static byte[] safeGetContent(SerializableContent object, Streamer streamer) {
        try {
            return getContent(object, streamer);
        } catch (Exception e) {
	  _log.error("unexpected problem whilst marshalling", e);
            return null;
        }
    }

    public static SerializableContent setContent(SerializableContent object, byte[] content, Streamer streamer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais=new ByteArrayInputStream(content);
        ObjectInput oi=streamer.getInputStream(bais);
        object.readContent(oi);
        oi.close();
        return object;
    }

    public static SerializableContent safeSetContent(SerializableContent object, byte[] content, Streamer streamer) {
        try {
            return setContent(object, content, streamer);
        } catch (Exception e) {
	  _log.error("unexpected problem whilst marshalling", e);
            return null;
        }
    }

    public static void safePut(Object item, Puttable puttable) {
        do {
            try {
                puttable.put(item);
            } catch (InterruptedException e) {
                if (_log.isTraceEnabled()) _log.trace("unexpected interruption - ignoring", e);
            }
        } while (Thread.interrupted());
    }

    public static Object safeTake(Takable takable) {
        do {
            try {
                return takable.take();
            } catch (InterruptedException e) {
                if (_log.isTraceEnabled()) _log.trace("unexpected interruption - ignoring", e);
            }
        } while (Thread.interrupted());

        throw new IllegalStateException();
    }

    public static void safeSleep(long period) {
        long end=System.currentTimeMillis()+(period);
        do {
            try {
                Thread.sleep(end-System.currentTimeMillis());
            } catch (InterruptedException e) {
                // ignore
            }
        } while (Thread.interrupted());
     }

    public static void safeAcquire(Sync sync) {
        do {
            try {
                sync.acquire();
            } catch (InterruptedException e) {
                if (_log.isTraceEnabled()) _log.trace("unexpected interruption - ignoring", e);
            }
        } while (Thread.interrupted());
    }

    public static File createTempDirectory(String prefix, String suffix, File directory) throws IOException {
    	File dir=File.createTempFile(prefix, suffix, directory);
    	dir.delete();
    	dir.mkdir();
    	return dir;
    }

}
