    <%@ page language="java" contentType="text/html" session="true" %>
      <%@ page import="java.util.Date" %>
      <%@ page import="java.util.Enumeration" %>
      <%
      System.out.println("FIND: "+session.getId());
      String colour=System.getProperty("wadi.colour");
      %>
      <HTML>
	<BODY BGCOLOR="<%= colour %>">
	  <H2>Session Find</H2>

	  <pre>id:            <%= session.getId() %></pre>
	  <pre>created:       <%= new Date(session.getCreationTime()) %></pre>
	  <pre>last accessed: <%= new Date(session.getLastAccessedTime()) %></pre>

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

