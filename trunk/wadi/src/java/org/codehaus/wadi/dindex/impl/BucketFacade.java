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
package org.codehaus.wadi.dindex.impl;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.Bucket;
import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.impl.MessageDispatcher;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

public class BucketFacade extends AbstractBucket {

    protected static final Log _log = LogFactory.getLog(BucketFacade.class);

    protected final ReadWriteLock _lock=new WriterPreferenceReadWriteLock();
    protected final LinkedQueue _queue=new LinkedQueue();

    protected boolean _queueing;
    protected long _timeStamp;
    protected Bucket _content;

    public BucketFacade(int key, long timeStamp, Bucket content, boolean queueing) {
        super(key);
        _timeStamp=timeStamp;
        _content=content;
        _queueing=queueing;
        _log.info("["+_key+"] initialising location to: "+_content);
    }

    public boolean isLocal() { // locking ?
        Sync sync=_lock.writeLock();
        boolean acquired=false;
        try {
            sync.acquire();
            acquired=true;

            return _content.isLocal();

        } catch (InterruptedException e) {
            _log.warn("unexpected problem", e);
        } finally {
            if (acquired)
                sync.release();
        }

        throw new UnsupportedOperationException();
    }

    public Bucket getContent() {
        Sync sync=_lock.writeLock();
        boolean acquired=false;
        try {
            sync.acquire();
            acquired=true;

            return _content;

        } catch (InterruptedException e) {
            _log.warn("unexpected problem", e);
        } finally {
            if (acquired)
                sync.release();
        }

        throw new UnsupportedOperationException();
    }

    public void setContent(long timeStamp, Bucket content) {
        Sync sync=_lock.writeLock();
        boolean acquired=false;
        try {
            sync.acquire();
            acquired=true;

            if (timeStamp>_timeStamp) {
                _log.info("["+_key+"] changing location from: "+_content+" to: "+content);
                _timeStamp=timeStamp;
                _content=content;
            }

        } catch (InterruptedException e) {
            _log.warn("unexpected problem", e);
        } finally {
            if (acquired)
                sync.release();
        }
    }

    public void setContentRemote(long timeStamp, MessageDispatcher dispatcher, Destination location) {
        Sync sync=_lock.writeLock();
        boolean acquired=false;
        try {
            sync.acquire();
            acquired=true;

            if (timeStamp>_timeStamp) {
                _timeStamp=timeStamp;
                if (_content instanceof RemoteBucket) {
                    ((RemoteBucket)_content).setLocation(location);
                } else {
                    _log.info("["+_key+"] changing location from: "+_content+" to: "+location);
                    _content=new RemoteBucket(_key, dispatcher, location);
                }
            }

        } catch (InterruptedException e) {
            _log.warn("unexpected problem", e);
        } finally {
            if (acquired)
                sync.release();
        }
    }
    
    public void dispatch(ObjectMessage om, DIndexRequest request) {
        // two modes:
        // direct dispatch
        // queued dispatch
        Sync sync=_lock.readLock();
        boolean acquired=false;
        try {
            sync.acquire();
            acquired=true;
            if (!_queueing) {
                _content.dispatch(om, request);
            }
            else
                _queue.put(om);
        } catch (InterruptedException e) {
            _log.warn("unexpected problem", e);
        } finally {
            if (acquired)
                sync.release();
        }
    }

    public boolean enqueue() {
        // decouple dispatch on content with a queue
        // all further input will be queued
        Sync sync=_lock.writeLock();
        boolean acquired=false;
        boolean success=false;
        try {
            sync.acquire();
            acquired=true;
            if (!_queueing) {
                _queueing=true;
                success=true;
            }
        } catch (InterruptedException e) {
            _log.warn("unexpected problem", e);
        } finally {
            if (acquired)
                sync.release();
        }
        return success;
    }

    public boolean dequeue() {
        // empty queue content into _content and recouple input directly...
        Sync sync=_lock.writeLock();
        boolean acquired=false;
        boolean success=false;
        try {
            sync.acquire();
            acquired=true;
            if (_queueing) {
                _queueing=false;
                while(!_queue.isEmpty()) {
                    try {
                        ObjectMessage message=(ObjectMessage)_queue.take();
                        DIndexRequest request=(DIndexRequest)message.getObject();
                        _content.dispatch(message, request); // perhaps this should be done on another thread ? - TODO
                    } catch (JMSException e) {
                        _log.warn("unexpected problem dispatching message");
                    }
                }
                success=true;
            }
        } catch (InterruptedException e) {
            _log.warn("unexpected problem", e);
        } finally {
            if (acquired)
                sync.release();
        }
        return success;
    }

}
