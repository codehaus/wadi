<html>
	<head>
		<g:render template="/layouts/head" />
	</head>
	<body>

    <div id="overall-screen">
		<g:render template="/layouts/header" />

		<div id="sidebar">
			<g:render template="/layouts/sidebar" />
		</div>
    
    	<div id="main">
			<g:render template="/layouts/mainTitle" />

			<g:layoutBody />		
    		<p/>
    		
			<g:form controller="${pageProperty(name:'meta.controller')}" action="${pageProperty(name:'meta.action')}">
			<table>
				<tr>
					<th style="width:40%">Service Space Names</th>
					<td>
						<g:select name="serviceSpaceName" from="${serviceSpaceNames}"/>
						<g:submitToRemote 
							update="[success:'successWrapper',failure:'failureWrapper']"
							action="${pageProperty(name:'meta.action')}"
							onComplete="new AutoRefreshManager('handle', 'track').observeSlider()"
							value="Display"/>
					</td>
				</tr>
		    </table>
			</g:form>

			<div id="successWrapper"></div>
			<div id="failureWrapper"></div>
		</div>

		<div id="footer"/>
    </div>
			
	</body>	
</html>