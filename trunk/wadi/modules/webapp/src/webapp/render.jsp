    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Enumeration" %>
      <%@ page import="java.util.SortedMap" %>
      <%@ page import="java.util.TreeMap" %>
      <%@ page import="java.util.Iterator" %>
      <%
      String colour=System.getProperty("node.name");
      String refresh=request.getParameter("refresh");

      if (refresh!=null)
      {
      String url=request.getRequestURL().toString();
      int i=url.indexOf(";jsessionid=");
      if (i!=-1)
      url=url.substring(0, i);
      String id=session.getId();
      if (id!=null)
      url+=";jsessionid="+id;
      url+="?refresh="+refresh;
      %>
    <meta http-equiv="refresh" content="<%= refresh %>;url=<%= url %>"/>
    <%
    }

    SortedMap entries=new TreeMap();
    synchronized (session) {
      for (Enumeration e=session.getAttributeNames(); e.hasMoreElements();) {
        Object key=e.nextElement();
        entries.put(key, session.getAttribute((String)key));
      }
    }

    int history=entries.size();
    int rows=1;
    int cols=1;

    if (history>1) {
      rows=(int)Math.sqrt((double)history);
      cols=history/rows;
      if (cols*rows<history)
        rows++;
    }
      %>

      <HTML>
	<BODY BGCOLOR="<%= colour %>">
      <center>
	<table border="2">
	  <%
	  int r=0;
	  Iterator v=entries.values().iterator();
	  while (r++<rows) {
	    int c=0;
	    %>
	    <tr>
	      <%
	      while (c++<cols) {
		Object bg="black";

		if (v.hasNext())
		  bg=(String)v.next();

		%>
		<td bgcolor="<%= bg %>" width="25" height="25"/>
		  <%
		  }
		  %>
	    </tr>
	    <%
	    }
	    %>
	</table>
      </center>
  </BODY>
    </HTML>

    <%
      response.setHeader("Connection", "close");
      %>

