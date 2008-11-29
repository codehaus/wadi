import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

grailsHome = Ant.project.properties."environment.GRAILS_HOME"

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  

target('default': "Unpack Dojo into web-app directory") {
    def stagingDir = 'target/dojo'
    
	Ant.mkdir(dir:stagingDir)
	Ant.gunzip(src:'scripts/dojo-release-1.0.2.tar.gz', dest:stagingDir)
	Ant.untar(src:"${stagingDir}/dojo-release-1.0.2.tar", dest:stagingDir)
	Ant.move(todir:'web-app/js/dojo') {
	    fileset(dir:"${stagingDir}/dojo-release-1.0.2", includes:"**")
	}
}
