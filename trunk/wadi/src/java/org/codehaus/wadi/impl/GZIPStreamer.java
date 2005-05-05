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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.codehaus.wadi.Streamer;

/**
 * Pluggable support for [un]GZIP-ing sessions as they are exchanged with
 * peers or long-term storage mechanisms.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class
  GZIPStreamer
  implements Streamer
{
  public ObjectInput getInputStream(InputStream is) throws IOException {return new ObjectInputStream(new GZIPInputStream(is));}
  public ObjectOutput getOutputStream(OutputStream os) throws IOException {return new ObjectOutputStream(new GZIPOutputStream(os));}
  public String getSuffix(){return "gz";}
}

