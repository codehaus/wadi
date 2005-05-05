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
package org.codehaus.wadi.sandbox.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.store.vm.VMPersistenceAdapter;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;
import org.codehaus.wadi.sandbox.SerializableContent;
import org.codehaus.wadi.sandbox.Streamer;

import EDU.oswego.cs.dl.util.concurrent.Sync;
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
	 * @param id - the id of said data
	 * @return - the resulting immotable - in other words - the data's new representation in the target Contextualiser
	 */
	public static Motable mote(Emoter emoter, Immoter immoter, Motable emotable, String id) {
		long startTime=System.currentTimeMillis();
		Motable immotable=immoter.nextMotable(id, emotable);
		boolean i=false;
		boolean e=false;
		if (((e=emoter.prepare(id, emotable, immotable) && (e=true))) && (immoter.prepare(id, emotable, immotable) && (i=true))) {
			immoter.commit(id, immotable);
			emoter.commit(id, emotable);
			long elapsedTime=System.currentTimeMillis()-startTime;
			if (_log.isDebugEnabled())_log.debug("motion: "+id+" : "+emoter.getInfo()+" -> "+immoter.getInfo()+" ("+elapsedTime+" millis)");
			return immotable;
		} else {
			if (e) emoter.rollback(id, emotable);
			if (i) immoter.rollback(id, immotable);
			long elapsedTime=System.currentTimeMillis()-startTime;
			if (_log.isWarnEnabled()) _log.warn("motion failed: "+id+" : "+emoter.getInfo()+" -> "+immoter.getInfo()+" ("+elapsedTime+" millis)");
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

    public static ActiveMQConnectionFactory getConnectionFactory() {
        // _connectionFactory=new ActiveMQConnectionFactory("peer://WADI-TEST");
        // _connectionFactory=new ActiveMQConnectionFactory("multicast://224.1.2.3:5123");
        // _connectionFactory=new ActiveMQConnectionFactory("jgroups:default");
        ActiveMQConnectionFactory cf=new ActiveMQConnectionFactory("tcp://localhost:61616");
        cf.setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter())); // peer protocol seems to ignore this...
        System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName()); // peer protocol sees this
        return cf;
    }

}
