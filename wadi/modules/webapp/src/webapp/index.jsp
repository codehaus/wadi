    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Enumeration" %>
      <%@ page import="java.util.TreeSet" %>
      <%@ page import="java.util.Iterator" %>
      <%@ page import="java.net.URL" %>
      <%
      String colour=System.getProperty("node.name");

      URL tmp=new URL(request.getRequestURL().toString());
      int port=tmp.getPort();
      String url=tmp.toString();

      String params="";

      String limit=request.getParameter("limit");
      if (limit!=null)
      params+=(params.length()==0?"?":"&")+"limit="+limit;

      String refresh=request.getParameter("refresh");
      if (refresh!=null)
      params+=(params.length()==0?"?":"&")+"refresh="+refresh;

      if (params.length()>0)
      {
      url+=params;
      %>
    <meta http-equiv="refresh" content="<%= refresh %>;url=<%= url %>"/>
    <%
    }

    url="./render.jsp";

    // add a new entry to session
    synchronized(session)
    {
    session.setAttribute(""+System.currentTimeMillis(), colour);
    }

    // if user has specified a limit to session size, remove from the
    // front to maintain this size - useful for running jmeter for long
    // periods...
    if (limit!=null)
    {
    int l=Integer.parseInt(limit);
    TreeSet keys=new TreeSet();

    synchronized (session)
    {
    for (Enumeration e=session.getAttributeNames(); e.hasMoreElements();)
    keys.add(e.nextElement());
    }

    if (keys.size()>l)
    {
    int sessionSize=keys.size();
    for (Iterator i=keys.iterator(); sessionSize-->l && i.hasNext();)
    synchronized(session){session.removeAttribute((String)i.next());}
    }
    }
      %>

    <HTML>
      <frameset rows="33%,33%,34%" cols="33%,33%,34%">
	<frame src="<%= url %>"/>
	  <frame src="<%= url %>"/>
	    <frame src="<%= url %>"/>
	      <frame src="<%= url %>"/>
		<frame src="<%= url %>"/>
		  <frame src="<%= url %>"/>
		    <frame src="<%= url %>"/>
		      <frame src="<%= url %>"/>
			<frame src="<%= url %>"/>
      </frameset>
    </HTML>

    <%
      response.setHeader("Connection", "close");
      %>
