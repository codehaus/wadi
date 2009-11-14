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
		</div>

		<div id="footer"/>
    </div>
			
	</body>	
</html>