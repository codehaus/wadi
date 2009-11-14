<%@ page language="java" contentType="text/html" session="true" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.net.URL" %>
<%@ page import="org.codehaus.wadi.webapp.Counter" %>
<%
  String colour=System.getProperty("node.name");

  URL tmp=new URL(request.getRequestURL().toString());
  String url=tmp.toString();

  String params="";

  String limit=request.getParameter("limit");
  int l=-1;
  if (limit!=null) {
    l=Integer.parseInt(limit);
    params+=(params.length()==0?"?":"&")+"limit="+limit;
  }

  String refresh=request.getParameter("refresh");
  if (refresh!=null) {
    params+=(params.length()==0?"?":"&")+"refresh="+refresh;
  }

  if (params.length()>0) {
    url+=params;
%>
<meta http-equiv="refresh" content="<%= refresh %>;url=<%= url %>"/>
<%
}

url="./session.jsp;jsessionid=" + session.getId() + "?insert=false";

// acquire session history
Counter counter=null;
LinkedList history=null;

synchronized (session) {
  if ((counter=(Counter)session.getAttribute("counter"))==null) {
    session.setAttribute("counter", (counter=new Counter()));
  }

  if ((history=(LinkedList)session.getAttribute("history"))==null) {
    session.setAttribute("history", (history=new LinkedList()));
  }
}

synchronized (history) {

  // add a new history item to end of queue
  counter.increment();
  history.addLast(new Object[]{new Integer(counter.getValue()), colour});
  // remove an old item from the beginning if history is getting too long
  if (l>=0 && history.size()>l) {
    history.removeFirst();
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
