import com.sap.piper.BuildTool
import com.sap.piper.DownloadCacheUtils
import groovy.transform.Field
import groovy.io.FileType

import static com.sap.piper.Prerequisites.checkScript

@Field String METADATA_FILE = 'metadata/mavenBuild.yaml'
@Field String STEP_NAME = getClass().getName()

void call(Map parameters = [:]) {
    List credentials = [[type: 'token', id: 'altDeploymentRepositoryPasswordId', env: ['PIPER_altDeploymentRepositoryPassword']]]
    final script = checkScript(this, parameters) ?: this
    parameters = DownloadCacheUtils.injectDownloadCacheInParameters(script, parameters, BuildTool.MAVEN)
	println("DEBUGGING ---- START ----")
	def list = []

	def dir = new File("./")
	dir.eachFileRecurse (FileType.FILES) { file ->
	  list << file
	}
	list.each {
	  println it.path
	}
	println("DEBUGGING ---- END ----")
    piperExecuteBin(parameters, STEP_NAME, METADATA_FILE, credentials)
}
