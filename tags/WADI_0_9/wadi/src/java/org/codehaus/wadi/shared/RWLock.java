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

/*
  File: WriterPreferenceReadWriteLock.java

  Originally written by Doug Lea and released into the public domain.
  This may be used for any purposes whatsoever without acknowledgment.
  Thanks for the assistance and support of Sun Microsystems Labs,
  and everyone contributing, testing, and using this code.

  History:
  Date       Who                What
  11Jun1998  dl               Create public version
   5Aug1998  dl               replaced int counters with longs
  25aug1998  dl               record writer thread
   3May1999  dl               add notifications on interrupt/timeout

*/

package org.codehaus.wadi.shared;

// This started off life as a straight copy of Doug Lea's
// WriterPreferenceReadWriteLock in
// EDU.oswego.cs.dl.util.concurrent... I will be refactoring it to add
// priority ordering of writers and lock overlapping...

// Doug's code is under whatever license he chose, mine is under ASF2

import EDU.oswego.cs.dl.util.concurrent.*;

/**
 * A read-write lock. Writers are preferred. Writers are organised
 * according to 'priority'. A Reader may overlap release of its read
 * lock with its application for a write lock.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class RWLock implements ReadWriteLock {

  protected int         _maxPriority=Thread.MAX_PRIORITY;
  protected ThreadLocal _priority=new ThreadLocal(){protected synchronized Object initialValue() {return new Integer(0);}};
  protected class Lock {int _count=0;}

  protected long activeReaders_ = 0;
  protected Thread activeWriter_ = null;
  protected long waitingReaders_ = 0;
  protected long waitingWriters_ = 0;

  public RWLock(int maxPriority){_maxPriority=maxPriority;}

  public void setPriority(int priority){_priority.set(new Integer(priority));}
  public int getPriority(){return ((Integer)_priority.get()).intValue();}

  protected final ReaderLock readerLock_ = new ReaderLock();
  protected final WriterLock writerLock_ = new WriterLock();

  public Sync writeLock() { return writerLock_; }
  public Sync readLock() { return readerLock_; }

  /*
    A bunch of small synchronized methods are needed
    to allow communication from the Lock objects
    back to this object, that serves as controller
  */


  protected synchronized void cancelledWaitingReader() { --waitingReaders_; }
  protected synchronized void cancelledWaitingWriter(Lock l) { --waitingWriters_; l._count--;}


  protected boolean allowReader() {
    return activeWriter_ == null;
  }


  protected synchronized boolean startRead() {
    boolean allowRead = allowReader();
    if (allowRead)  ++activeReaders_;
    return allowRead;
  }

  protected synchronized boolean startWrite() {

    // The allowWrite expression cannot be modified without
    // also changing startWrite, so is hard-wired

    boolean allowWrite = (activeWriter_ == null && activeReaders_ == 0);
    if (allowWrite)  activeWriter_ = Thread.currentThread();
    return allowWrite;
   }


  /*
     Each of these variants is needed to maintain atomicity
     of wait counts during wait loops. They could be
     made faster by manually inlining each other. We hope that
     compilers do this for us though.
  */

  protected synchronized boolean startReadFromNewReader() {
    boolean pass = startRead();
    if (!pass) ++waitingReaders_;
    return pass;
  }

  protected synchronized boolean startWriteFromNewWriter(Lock l) {
    boolean pass = startWrite();
    if (!pass)
    {
      ++waitingWriters_;
      l._count++;
    }
    return pass;
  }

  protected synchronized boolean startReadFromWaitingReader() {
    boolean pass = startRead();
    if (pass) --waitingReaders_;
    return pass;
  }

  protected synchronized boolean startWriteFromWaitingWriter(Lock l) {
    boolean pass = startWrite();
    if (pass)
    {
      --waitingWriters_;
      l._count--;
    }
    return pass;
  }

  /**
   * Called upon termination of a read.
   * Returns the object to signal to wake up a waiter, or null if no such
   **/
  protected synchronized Signaller endRead() {
    if (--activeReaders_ == 0 && waitingWriters_ > 0)
      return writerLock_;
    else
      return null;
  }


  /**
   * Called upon termination of a write.
   * Returns the object to signal to wake up a waiter, or null if no such
   **/
  protected synchronized Signaller endWrite() {
    activeWriter_ = null;
    if (waitingReaders_ > 0 && allowReader())
      return readerLock_;
    else if (waitingWriters_ > 0)
      return writerLock_;
    else
      return null;
  }


  /**
   * Reader and Writer requests are maintained in two different
   * wait sets, by two different objects. These objects do not
   * know whether the wait sets need notification since they
   * don't know preference rules. So, each supports a
   * method that can be selected by main controlling object
   * to perform the notifications.  This base class simplifies mechanics.
   **/

  protected abstract class Signaller  { // base for ReaderLock and WriterLock
    abstract void signalWaiters();
  }

  protected class ReaderLock extends Signaller implements Sync {

    public  void acquire() throws InterruptedException {
      if (Thread.interrupted()) throw new InterruptedException();
      InterruptedException ie = null;
      synchronized(this) {
        if (!startReadFromNewReader()) {
          for (;;) {
            try {
              ReaderLock.this.wait();
              if (startReadFromWaitingReader())
                return;
            }
            catch(InterruptedException ex){
              cancelledWaitingReader();
              ie = ex;
              break;
            }
          }
        }
      }
      if (ie != null) {
        // fall through outside synch on interrupt.
        // This notification is not really needed here,
        //   but may be in plausible subclasses
        writerLock_.signalWaiters();
        throw ie;
      }
    }


    public void release() {
      Signaller s = endRead();
      if (s != null) s.signalWaiters();
    }


    synchronized void signalWaiters() { ReaderLock.this.notifyAll(); }

    public boolean attempt(long msecs) throws InterruptedException {
      if (Thread.interrupted()) throw new InterruptedException();
      InterruptedException ie = null;
      synchronized(this) {
        if (msecs <= 0)
          return startRead();
        else if (startReadFromNewReader())
          return true;
        else {
          long waitTime = msecs;
          long start = System.currentTimeMillis();
          for (;;) {
            try { ReaderLock.this.wait(waitTime);  }
            catch(InterruptedException ex){
              cancelledWaitingReader();
              ie = ex;
              break;
            }
            if (startReadFromWaitingReader())
              return true;
            else {
              waitTime = msecs - (System.currentTimeMillis() - start);
              if (waitTime <= 0) {
                cancelledWaitingReader();
                break;
              }
            }
          }
        }
      }
      // safeguard on interrupt or timeout:
      writerLock_.signalWaiters();
      if (ie != null) throw ie;
      else return false; // timed out
    }

  }

  protected class WriterLock extends Signaller implements  Sync {

    Lock[] _locks=new Lock[_maxPriority+1];

    WriterLock()
    {
      for (int i=0;i<=_maxPriority;i++)
	_locks[i]=new Lock();
    }

    public void acquire() throws InterruptedException {
      if (Thread.interrupted()) throw new InterruptedException();
      InterruptedException ie = null;
      int p=getPriority();
      Lock l=_locks[p];
      synchronized(l) {
        if (!startWriteFromNewWriter(l)) {
          for (;;) {
            try {
              l.wait();
              if (startWriteFromWaitingWriter(l))
                return;
            }
            catch(InterruptedException ex){
              cancelledWaitingWriter(l);
              l.notify();
              ie = ex;
              break;
            }
          }
        }
      }
      if (ie != null) {
        // Fall through outside synch on interrupt.
        //  On exception, we may need to signal readers.
        //  It is not worth checking here whether it is strictly necessary.
        readerLock_.signalWaiters();
        throw ie;
      }
    }

    public void release(){
      Signaller s = endWrite();
      if (s != null) s.signalWaiters();
    }

    synchronized void
      signalWaiters()
    {
      // walk down from top priority looking for a thread to notify...
      for (int i=_maxPriority;i>=0;i--)
      {
	Lock l=_locks[i];
	synchronized (l)
	{
	  if (l._count>0)
	  {
	    l.notify();
	    return;
	  }
	}
      }
    }

    public boolean attempt(long msecs) throws InterruptedException {
      if (Thread.interrupted()) throw new InterruptedException();
      InterruptedException ie = null;
      int p=getPriority();
      Lock l=_locks[p];
      synchronized(l) {
        if (msecs <= 0)
          return startWrite();
        else if (startWriteFromNewWriter(l))
          return true;
        else {
          long waitTime = msecs;
          long start = System.currentTimeMillis();
          for (;;) {
            try { l.wait(waitTime);  }
            catch(InterruptedException ex){
              cancelledWaitingWriter(l);
              l.notify();
              ie = ex;
              break;
            }
            if (startWriteFromWaitingWriter(l))
              return true;
            else {
              waitTime = msecs - (System.currentTimeMillis() - start);
              if (waitTime <= 0) {
                cancelledWaitingWriter(l);
		l.notify();
                break;
              }
            }
          }
        }
      }

      readerLock_.signalWaiters();
      if (ie != null) throw ie;
      else return false; // timed out
    }

  }


  // Not well tested - I'm concerned about the synchronisation...
  public void
    overlap()
    throws InterruptedException
  {
    synchronized (RWLock.this)
    {
      Signaller s=endRead();

      if (s==null)
      {
	// there are still extant readers - this call to acquire will
	// just queue a write lock - it could be optimised but...
	writeLock().acquire();
      }
      else
      {
	// readers are exhausted, we don't want to let this writer
	// jump straight into the gap as it may not be of a higher
	// priority than the other waiting writers...

	// WARNING - TODO - However, at the moment we KNOW that the
	// only thread using the overlap method will be the
	// invalidation request thread so we can live with this -
	// revisit if we ever need this lock to be used in a different
	// manner...
	writeLock().acquire();
      }
    }
  }

  public String
    toString()
  {
    return "<RWLock:"+this.hashCode()+":activeReaders:"+activeReaders_+", waitingReaders:"+waitingReaders_+", activeWriter:"+(activeWriter_!=null)+", waitingWriters:"+waitingWriters_+">";
  }
}

