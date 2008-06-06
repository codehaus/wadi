<%
def displayLink = {controllerName, actionName, title ->
	def currentPageTitle = pageProperty(name:'meta.title')
	def link;
	if (currentPageTitle == title) {
		link = "${title}<br/>"
	} else {
		link = "<a href='${createLink(controller:controllerName, action:actionName)}'>${title}</a><br/>"
	}
	link
}
%>

${displayLink('selectCluster', 'list', 'Clusters')}
${displayLink('listServiceSpace', 'list', 'Service Spaces')}
${displayLink('monitorGlobal', 'monitor', 'Monitor Global Envelopes')}
${displayLink('monitorCounting', 'listServiceSpaceNames', 'Monitor Service Space Envelopes')}
${displayLink('retrieveContextualiserStack', 'listServiceSpaceNames', 'Contextualiser Stack')}
${displayLink('retrievePartitionBalancing', 'listServiceSpaceNames', 'Partition Balancing')}
${displayLink('listSession', 'listServiceSpaceNames', 'Sessions')}