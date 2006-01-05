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

package org.codehaus.wadi.sandbox.cluster;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  CollectionComparator
  implements java.util.Comparator
{
  public int
    compare(Object o1, Object o2)
    {
      Collection ts1=(Collection)o1;
      Collection ts2=(Collection)o2;

      Iterator i=ts1.iterator();
      Iterator j=ts2.iterator();

      int result=0;
      while (i.hasNext() && j.hasNext() && result==0)
      {
	result=((Comparable)i.next()).compareTo(j.next());
      }

      if (result!=0)
	return result;
      else if (i.hasNext())
	return -1;
      else if (j.hasNext())
	return 1;
      else
	return 0;
    }

  public boolean
    equals(Object o)
    {
      return this==o;
    }
}
