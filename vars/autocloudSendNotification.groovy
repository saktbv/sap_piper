void call(Map parameters = [:]) {
	def emailBody = """     Hello,</br></br>
							${env.job_name} - Build # $BUILD_NUMBER ${currentBuild.currentResult}</br></br>
							Please check console output <a href='$BUILD_URL'>here</a> to view the details.</br></br></br>
							Regards,</br>
							COS4Auto DevOps Team
                    """
	def emailSubject = "${env.JOB_NAME}- Jenkins Build ${currentBuild.currentResult}"
		
	emailext body: "${emailBody}", subject: "${emailSubject}", to: "${env.GIT_LAST_COMMITTER_EMAIL}"
	
}
return this
