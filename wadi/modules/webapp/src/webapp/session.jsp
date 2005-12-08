    <%@ page language="java" contentType="text/html" session="true" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.SortedMap" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.codehaus.wadi.webapp.Counter" %>
<%
  String colour=System.getProperty("node.name");

  String insert=request.getParameter("insert");
  if (insert==null) {
    insert="true";
  }

  String limit=request.getParameter("limit");
  int l=-1;
  if (limit!=null)
  l=Integer.parseInt(limit);

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

// acquire session history
Counter counter=null;
SortedMap history=null;

synchronized (session) {
  if ((counter=(Counter)session.getAttribute("counter"))==null) {
    session.setAttribute("counter", (counter=new Counter()));
  }

  if ((history=(SortedMap)session.getAttribute("history"))==null) {
    session.setAttribute("history", (history=new TreeMap()));
  }
}

if (history.size()==0)
System.out.println("KEY: "+session.getId()+", HISTORY: "+history+" - "+Thread.currentThread().getName());

if (insert!=null && insert.equals("true")) {
  synchronized (history) {

    // add a new history item to end of queue
    counter.increment();
    history.put(new Integer(counter.getValue()), colour);
    // remove an old item from the beginning if history is getting too long
    if (l>=0 && history.size()>l) {
      Iterator i=history.entrySet().iterator();
      i.next();
      i.remove();
    }
  }

  // take our own thread-local copy
  history=new TreeMap(history);
}



int hsize=history.size();
int rows=1;
int cols=1;

if (hsize>1) {
  rows=(int)Math.sqrt((double)hsize);
  cols=hsize/rows;
  if (cols*rows<hsize)
    rows++;
  }
  %>

  <HTML>
    <BODY BGCOLOR="<%= colour %>">
      <center>
        <table border="2">
          <%
            int r=0;
            Iterator v=history.entrySet().iterator();
            while (r++<rows) {
                int c=0;
            %>
            <tr>
              <%
                while (c++<cols) {
                    Object label="";
                    Object bg="black";
                    if (v.hasNext()) {
                      Map.Entry entry=(Map.Entry)v.next();
                      label=entry.getKey();
                      bg=entry.getValue().toString();
                    }

                %>
                <td align="center" bgcolor="<%= bg %>" width="25" height="25"><%= label %></td>
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

  <%
    response.setHeader("Connection", "close");
  %>
