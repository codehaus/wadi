package org.codehaus.wadi.impl;

import java.util.Random;

import org.codehaus.wadi.SessionIdFactory;

public class FixedWidthSessionIdFactory implements SessionIdFactory {

	public String create() {
		int width=_keyLength;
		char[] buffer=new char[width];
		
		int offset=width-1;
		while ((offset=encode(Math.abs(_random.nextLong()), _sectLength, buffer, offset))>=0);
		
		return new String(buffer);
	}

	public int getSessionIdLength() {
		return _keyLength+_divider.length+_bucketLength;
	}

	public void setSessionIdLength(int l) {
		//_width=l-_divider.length-_bucketSize;
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
  protected final int _numBuckets;
  protected final int _bucketLength;

  public FixedWidthSessionIdFactory(int width, int numBuckets) {
    this(width, _defaultChars, numBuckets);
  }

  public FixedWidthSessionIdFactory(int width, char[] chars, int numBuckets) {
    _keyLength=width;
    _chars=chars;
    _base=_chars.length;
    _numBuckets=numBuckets;
    _bucketLength=size(_numBuckets);

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

  public String create(int bucket) {
    int width=_keyLength+_divider.length+_bucketLength;
    char[] buffer=new char[width];

    int offset=width-1;
    offset=encode(bucket, _bucketLength, buffer, offset);

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
    return decode(key.toCharArray(), key.length()-_bucketLength, _bucketLength);
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
    int numBuckets=1024;
    FixedWidthSessionIdFactory factory=new FixedWidthSessionIdFactory(32, numBuckets);
    Random r=new Random();
    for (int i=0; i<numBuckets; i++) {
      int bucket=Math.abs(r.nextInt())%numBuckets;
      String key=factory.create(bucket);
      System.out.println(key+" - "+bucket);
      assert bucket==factory.getPartition(key);
    }
  }

  // should we shuffle the _chars used for the key ?
  // cannot shuffle _chars for bucket - since must be the same on every node
  // rename name->key where dealing with sessions...

  // when we allocate a session, we need to decide which bucket to put it in
  // allocation and entry into bucket must be atomic - I guess the entry could chase the bucket...


}
