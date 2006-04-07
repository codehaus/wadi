    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Date" %>
      <%@ page import="java.util.Enumeration" %>
      <%@ page import="org.apache.commons.logging.Log" %>
      <%@ page import="org.apache.commons.logging.LogFactory" %>
      <%@ page import="org.codehaus.wadi.webapp.BigString" %>
      <%
      Log log=LogFactory.getLog(getClass());
      log.trace("CREATE(700k): "+session.getId());
      String colour=System.getProperty("node.name");
      %>

      <HTML>
	<BODY BGCOLOR="<%= colour %>">
	  <H2>Session Creation</H2>

	  <pre>current url:           <%= request.getRequestURL() %></pre>
	  <pre>session id:            <%= session.getId() %></pre>
	  <pre>session created:       <%= new Date(session.getCreationTime()) %></pre>
	  <pre>session last accessed: <%= new Date(session.getLastAccessedTime()) %></pre>

	  <p/>
	    <table>
	      <%
	      synchronized (session) {
	      session.setAttribute(""+session.getLastAccessedTime(), new BigString(colour, 700*1024));
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
	      }
	      %>
	    </table>
	</BODY>
      </HTML>

    <%
      response.setHeader("Connection", "close");
      %>