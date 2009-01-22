<html>
    <head>
        <meta name="layout" content="main"></meta>
        <meta name="title" content="ServiceSpace Monitoring of Envelope Exchanges"></meta>

        <% getCountingInfosAsJSONURL = "'../monitorJSON/${serviceSpaceName}'" %>
        <g:render template="/monitorGlobal/genericGraphJavaScript"
            model="[countingInfos:countingInfos, getCountingInfosAsJSONURL:getCountingInfosAsJSONURL]" />
    </head>
    <body>
    This page displays the number of envelopes, i.e. messages, exchanged by the ServiceSpace ${serviceSpaceName}
    on each hosting peer.<br>
    <g:render template="/monitorGlobal/genericGraphDescription" />
    <p/>

    <g:render template="/shared/refreshSlider" model="[track:'track', handle:'handle']" />
    <p/>

    <g:each in="${countingInfos}" var="countingInfo">
      <div id="${countingInfo.hostingPeer.name}">
        Peer ${countingInfo.hostingPeer.name}
        <div id="${countingInfo.hostingPeer.name}_GRAPH" style="width: 600px; height: 200px;"></div>
      </div>
    </g:each>
    </body>
</html>