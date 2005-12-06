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

import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.store.vm.VMPersistenceAdapter;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
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
		long startTime=System.currentTimeMillis();
		Motable immotable=immoter.nextMotable(name, emotable);
		boolean i=false;
		boolean e=false;
		if ((i=immoter.prepare(name, emotable, immotable)) &&(e=emoter.prepare(name, emotable, immotable))) {
			immoter.commit(name, immotable);
			emoter.commit(name, emotable);
			long elapsedTime=System.currentTimeMillis()-startTime;
			if (_log.isDebugEnabled())_log.debug("motion: "+name+" : "+emoter.getInfo()+" -> "+immoter.getInfo()+" ("+elapsedTime+" millis)");
			return immotable;
		} else {
			if (e) emoter.rollback(name, emotable);
			if (i) immoter.rollback(name, immotable);
			long elapsedTime=System.currentTimeMillis()-startTime;
			if (_log.isWarnEnabled()) _log.warn("motion failed: "+name+" : "+emoter.getInfo()+" -> "+immoter.getInfo()+" ("+elapsedTime+" millis)");
			return null;
		}
	}

	/**
	 * Ignore any interruptions whilst acquiring a lock.
	 *
	 * @param sync - the lock
	 * @throws TimeoutException - some Syncs (TimeoutSync) may timeout during acquisition
	 */
	public static void acquireUninterrupted(Sync sync) throws TimeoutException {
	    do {
	        try {
	            sync.acquire();
	        } catch (TimeoutException e) {
	            Thread.interrupted(); // TODO - not sure if we need to clear the interrupted flag ?
	            throw e; // a TimeoutException isa InterruptedException
	        } catch (InterruptedException e) {
		  _log.trace("unexpected interruption - ignoring", e);
	        }
	    } while (Thread.interrupted());
	}

	/**
	 * If a lock is free, acquire it, ignoring any interruptions, else fail.
	 *
	 * @param sync - the lock
	 * @return - whether or not the lock was acquired
	 */
	public static boolean attemptUninterrupted(Sync sync) {
	    boolean acquired=false;
	    do {
	        try {
	            acquired=sync.attempt(0);
	        } catch (InterruptedException e) {
		  _log.trace("unexpected interruption - ignoring", e);
	        }
	    } while (Thread.interrupted());
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

    public static String getClusterUri() {
        return "peer://org.codehaus.wadi";
    }

    // this should really be passed in the top somewhere - but the tests need access too...
    public static ActiveMQConnectionFactory getConnectionFactory() {
        ActiveMQConnectionFactory cf=new ActiveMQConnectionFactory(getClusterUri());
        // ActiveMQConnectionFactory cf=new ActiveMQConnectionFactory("tcp://localhost:61616");
        // _connectionFactory=new ActiveMQConnectionFactory("multicast://224.1.2.3:5123");
        // _connectionFactory=new ActiveMQConnectionFactory("jgroups:default");
        cf.setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter())); // peer protocol seems to ignore this...
        System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName()); // peer protocol sees this
        return cf;
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