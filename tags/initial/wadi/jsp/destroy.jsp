    <%@ page language="java" contentType="text/html" session="true" %>
    <%@ page import="java.util.Date" %>
      <HTML>
	<BODY>
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

