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

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.gridstate.PartitionMapper;

public class FixedWidthSessionIdFactory implements SessionIdFactory, PartitionMapper {

    protected final Log _log=LogFactory.getLog(getClass().getName());
    
	public String create() {
		int width=_keyLength;
		char[] buffer=new char[width];

		int offset=width-1;
		while ((offset=encode(Math.abs(_random.nextLong()), _sectLength, buffer, offset))>=0);

		return new String(buffer);
	}

	public int getSessionIdLength() {
		return _keyLength+_divider.length+_partitionLength;
	}

	public void setSessionIdLength(int l) {
		//_width=l-_divider.length-_partitionSize;
	}

  //protected final static char[] _defaultChars="0123456789".toCharArray();
  protected final static char[] _defaultChars="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

  protected final char[] _divider="-".toCharArray();
  //protected final char[] _divider="".toCharArray();
  protected Random _random=new Random();
  protected final int[] _lookup=new int[Character.MAX_VALUE];

  protected final int _keyLength;
  protected final char[] _chars;
  protected final int _base;
  protected final int _sectLength;
  protected final int _numPartitions;
  protected final int _partitionLength;

  public FixedWidthSessionIdFactory(int width, int numPartitions) {
    this(width, _defaultChars, numPartitions);
  }

  public FixedWidthSessionIdFactory(int width, char[] chars, int numPartitions) {
    _keyLength=width;
    _chars=chars;
    _base=_chars.length;
    _numPartitions=numPartitions;
    _partitionLength=size(_numPartitions);

    //    System.out.println("radix="+_radix);
    // initialise reverse lookup table...
    for (int i=0; i<_base; i++)
      _lookup[_chars[i]]=i;

    // calculate how many digits max-long would render in the given
    // base...
    //    System.out.println("max="+Long.MAX_VALUE);
    _sectLength=size(Long.MAX_VALUE);
    //    System.out.println("iters="+_iters);
  }

  public String create(int partition) {
    int width=_keyLength+_divider.length+_partitionLength;
    char[] buffer=new char[width];

    int offset=width-1;
    offset=encode(partition, _partitionLength, buffer, offset);

    for (int i=_divider.length-1; i>=0; i--)
      buffer[offset--]=_divider[i];

    while ((offset=encode(Math.abs(_random.nextLong()), _sectLength, buffer, offset))>=0);

    return new String(buffer);
  }

  protected int encode(long sect, int iters, char[] buffer, int offset) {
    for (int i=0; i<iters && offset>=0; i++) {
      //      System.out.println(""+i+" - "+source);
      buffer[offset--]=_chars[(int)(sect%_base)];
      sect/=_base;
    }
    return offset;
  }

  public int getPartition(String key) {
    return decode(key.toCharArray(), key.length()-_partitionLength, _partitionLength);
  }

  protected int decode(char[] buffer, int from, int length) {
    int result=0;
    int multiplier=1;
    for (int i=from+length-1; i>=from; i--) {
      //      System.out.println(""+buffer[i]+" - "+_lookup[buffer[i]]);
      result+=_lookup[buffer[i]]*multiplier;
      multiplier*=_base;
    }
    return result;
  }

  protected int size(long l) {
    int i=0;
    while (l!=0) {
      l/=_base;
      i++;
    }
    return i;
  }

  protected int size(int n) {
    int i=0;
    while (n!=0) {
      n/=_base;
      i++;
    }
    return i;
  }

  public static void main(String[] args) {
    int numPartitions=1024;
    FixedWidthSessionIdFactory factory=new FixedWidthSessionIdFactory(32, numPartitions);
    Random r=new Random();
    for (int i=0; i<numPartitions; i++) {
      int partition=Math.abs(r.nextInt())%numPartitions;
      String key=factory.create(partition);
      System.out.println(key+" - "+partition);
      assert partition==factory.getPartition(key);
    }
  }

  // PartitionMapper API

  public int map(Object key) {
	  int index=getPartition((String)key);
	  _log.info("mapped "+key+" -> "+index);
	  return index;
  }

  // should we shuffle the _chars used for the key ?
  // cannot shuffle _chars for partition - since must be the same on every node
  // rename name->key where dealing with sessions...

  // when we allocate a session, we need to decide which partition to put it in
  // allocation and entry into partition must be atomic - I guess the entry could chase the partition...


}
