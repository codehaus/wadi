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

package org.codehaus.wadi.shared;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class
  LogFormatter
  extends Formatter
{
  protected static final String newline=System.getProperty("line.separator");
  protected static final String[] _xlate=new String[]
    {
      "lvl0 ",
      "lvl1 ",
      "lvl2 ",
      "TRACE", // FINEST
      "finer", // FINER
      "DEBUG", // FINE
      "lvl6 ",
      "conf ", // CONFIG
      "INFO ", // INFO
      "WARN ", // WARNING
      "ERROR", // SEVERE
    };

  protected final Date       _date=new Date();
  protected final DateFormat _fmt =new SimpleDateFormat("hh:mm:ss.SSS");
  //  protected final DateFormat _fmt =new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

  public synchronized String
    format(LogRecord r)
  {
    StringBuffer buf=new StringBuffer(); // could reuse...

    _date.setTime(r.getMillis());
    buf.append(_fmt.format(_date));
    buf.append(" ");
    buf.append(_xlate[r.getLevel().intValue()/100]);
    buf.append(" [");
    String ln=r.getLoggerName();
    if (ln.startsWith("org.codehaus.wadi."))
      ln="o.c.w."+ln.substring(18);
    buf.append(ln);
    buf.append("] ");
    buf.append(formatMessage(r));
    Throwable t=r.getThrown();
    if (t!=null)
    {
      buf.append(newline);
      StringWriter sw=new StringWriter();
      PrintWriter pw=new PrintWriter(sw);
      t.printStackTrace(pw);
      pw.close();
      buf.append(sw.toString());
    }

    buf.append(newline);
    return buf.toString();
  }
}
