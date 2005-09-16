    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Date" %>
      <%@ page import="org.apache.commons.logging.Log" %>
      <%@ page import="org.apache.commons.logging.LogFactory" %>
      <%
      Log log=LogFactory.getLog(getClass());
      log.trace("DESTROY: "+session.getId());
      String colour=System.getProperty("node.name");
      %>
      <HTML>
	<BODY BGCOLOR="<%= colour %>">
	  <H2>Session Destruction</H2>

	  <pre>id:            <%= request.getSession(false).getId() %></pre>
	  <pre>created:       <%= new Date(request.getSession(false).getCreationTime()) %></pre>
	  <pre>last accessed: <%= new Date(request.getSession(false).getLastAccessedTime()) %></pre>

	  <%
	  HttpSession s=request.getSession(false);
	  s.invalidate();
	  %>

	</BODY>
      </HTML>

    <%
      response.setHeader("Connection", "close");
      %>
