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
      String id=session.getId();

      if (refresh!=null)
      {
      String url=request.getRequestURL().toString();
      int i=url.indexOf(";jsessionid=");
      if (i!=-1)
      url=url.substring(0, i);
      if (id!=null)
      url+=";jsessionid="+id;
      url+="?refresh="+refresh;
      %>
    <meta http-equiv="refresh" content="<%= refresh %>;url=<%= url %>"/>
    <%
      }

    String url="./render.jsp";

    // are we using cookie or url rewriting?
    String rewrite=request.getParameter("rewrite");
    if (rewrite!=null)
    {
    if (id!=null)
    url+=";jsessionid="+id+"?"+"rewrite=true";
    };

    if (refresh!=null)
    url+="?"+refresh;

    // add a new entry to session
    session.setAttribute(""+System.currentTimeMillis(), colour);

    // if user has specified a limit to session size, remove from the
    // front to maintain this size - useful for running jmeter for long
    // periods...
    String limit=request.getParameter("limit");
    if (limit!=null)
    {
    int l=Integer.parseInt(limit);
    TreeSet keys=new TreeSet();
    for (Enumeration e=session.getAttributeNames(); e.hasMoreElements();)
    keys.add(e.nextElement());

    if (keys.size()>l)
    {
    int sessionSize=keys.size();
    for (Iterator i=keys.iterator(); sessionSize-->l && i.hasNext();)
    session.removeAttribute((String)i.next());
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

