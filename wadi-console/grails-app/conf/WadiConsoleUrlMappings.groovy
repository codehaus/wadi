class WadiConsoleUrlMappings {
	static mappings = {
	  "/"(controller:"selectCluster", action:"list")
	  "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }
	}	
}
