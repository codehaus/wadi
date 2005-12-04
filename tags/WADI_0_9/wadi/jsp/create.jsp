    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Date" %>
      <%@ page import="java.util.Enumeration" %>
      <%@ page import="java.net.URL" %>
      <%
      String colour=System.getProperty("wadi.colour");
      String url=request.getRequestURL().toString();

      int i=url.indexOf(";jsessionid=");
      if (i!=-1)
       url=url.substring(0, i);

      String id=session.getId();
      if (id!=null)
       url+=";jsessionid="+id;

      int period=(int)(Math.random()*25);
      %>

      <meta http-equiv="refresh" content="<%= period %>;url=<%= url %>"/>

      <HTML>
	<BODY BGCOLOR="<%= colour %>">
	  <H2>Session Creation</H2>

	  <pre>current url:           <%= request.getRequestURL() %></pre>
	  <pre>wait period:           <%= period %></pre>
	  <pre>next url:              <%= url %></pre>
	  <pre>session id:            <%= session.getId() %></pre>
	  <pre>session created:       <%= new Date(session.getCreationTime()) %></pre>
	  <pre>session last accessed: <%= new Date(session.getLastAccessedTime()) %></pre>

	  <p/>
	    <table>
	      <%
	      session.setAttribute(""+session.getLastAccessedTime(), colour);
	      int j=0;
	      for (Enumeration e=session.getAttributeNames(); e.hasMoreElements() && j<15;j++)
	      {
	      String key=(String)e.nextElement();
	      %>
	      <tr>
		<th bgcolor="<%= session.getAttribute(key) %>">
		  <%=  new Date(Long.parseLong(key)) %>
		</th>
	      </tr>
	      <%
	      }
	      %>
	    </table>
	</BODY>
      </HTML>
