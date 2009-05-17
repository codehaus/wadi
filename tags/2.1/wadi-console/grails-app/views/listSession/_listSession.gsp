<p/>

<g:form controller="listSession" action="listTable" name="refreshForm">
<g:hiddenField name="serviceSpaceName" value="${serviceSpaceName}"/>
<g:submitToRemote 
	update="[success:'successTableWrapper',failure:'failureTableWrapper']"
	action="listTable"
	value="Refresh"
	name="refresh"/>
<p/>

<g:render template="/shared/refreshSlider" model="[track:'track', handle:'handle']" />
<p/>

<div id="successTableWrapper">
	<g:render template="listTable" model="[sessions: sessions]" />
</div>
</g:form>

<div id="failureTableWrapper"></div>