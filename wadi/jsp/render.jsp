    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Date" %>
      <%@ page import="java.util.Enumeration" %>
      <%@ page import="java.util.TreeSet" %>
      <%@ page import="java.util.Comparator" %>
      <%@ page import="java.util.Iterator" %>
      <%@ page import="java.net.URL" %>
      <%
      String colour=System.getProperty("wadi.colour");
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

//      session.setAttribute(""+session.getLastAccessedTime(), colour);
      String[] names=session.getValueNames();

      TreeSet keys=new TreeSet();
      for (Enumeration e=session.getAttributeNames(); e.hasMoreElements();)
      keys.add(e.nextElement());

      int history=keys.size();;
      int rows=(int)Math.sqrt((double)history);
      int cols=history/rows;
    if (cols*rows<history)
      rows++;

      %>

    <HTML>
      <BODY BGCOLOR="<%= colour %>">
	<center>
	<table border="2">
	  <%
	  int r=0;
	  int e=0;
	  Iterator k=keys.iterator();
	  while (r++<rows)
	    {
	    int c=0;
	    %>
	    <tr>
	      <%
	      while (c++<cols)
		{
		String bg="black";

		if (k.hasNext())
		bg=(String)session.getValue((String)k.next());

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

