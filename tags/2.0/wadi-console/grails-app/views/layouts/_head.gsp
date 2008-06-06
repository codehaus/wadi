<title><g:layoutTitle default="WADI - Management Console" /></title>
<link rel="stylesheet" href="${createLinkTo(dir:'css',file:'wadi_main.css')}"></link>
<g:javascript library="prototype" />
<g:javascript library="scriptaculous" />
<g:javascript library="application" />				


<g:javascript library="dojo/dojo/dojo.js.uncompressed" />
<script>
dojo.registerModulePath("dojo", "${createLinkTo(dir:'js/dojo',file:'dojo')}");
dojo.registerModulePath("dojox", "${createLinkTo(dir:'js/dojo',file:'dojox')}");
</script>
<g:javascript library="dojo/dojox/charting/Chart2D" />

<g:javascript library="wadi/filter" />
<g:javascript library="wadi/AutoRefreshManager" />
<g:layoutHead />