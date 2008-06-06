<%@ page language="java" contentType="text/html" session="true" %>
<%
String sessId = session.getId();
String color=System.getProperty("node.name");
session.setAttribute("foo", "bar");
%>
<html>
  <head>
    <title>WADI Set Page</title>
  </head>
  <body bgcolor="<%= color %>">
    <p>The session id is = <%= sessId %></p>
    <p>The value of foo was set to = <%= session.getAttribute("foo") %></p>
  </body>
</html>