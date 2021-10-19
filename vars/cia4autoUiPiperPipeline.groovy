void call(parameters) {
    //send email notification to admin when the build fails
    def emailTo = 'jyoti.chaudhury@capgemini.com'
	def sprint_number, org, target_repo, git_valid_message, git_commit_author, git_commit_email, git_commit_message
    pipeline {
        agent any
        triggers {
            issueCommentTrigger('.*/piper ([a-z]*).*')
        }
        options {
            skipDefaultCheckout()
            timestamps()
        }
        stages {
		    stage('Pre-Init'){
				steps{
					withCredentials([usernameColonPassword(credentialsId: 'GITHUB_CREDS', variable: 'github_credential')]){
					    deleteDir()
						checkout scm
						script{
							def properties = readProperties  file:'sprint.info'
							sprint_number = properties['sprint-number']
							org = properties['github-org']
							target_repo = properties['github-target-repo']
							git_valid_message = properties['git-commit-message-to-promote']
							git_commit_author = sh (
													script: 'git log -1 --format=%an',
													returnStdout: true
												).trim()
							echo "Last git committer user: ${git_commit_author}"	
							git_commit_email = sh (
													script: 'git --no-pager show -s --format=%ae',
													returnStdout: true
												).trim()
							echo "Last git committer email: ${git_commit_email}"
							git_commit_message = sh (
													script: 'git log -1 --pretty=%B',
													returnStdout: true
												).trim()
							echo "Last git commit message: ${git_commit_message}"
						}
					}
				}
			}
            stage('Init') {
                steps {
                    piperPipelineStageInit script: parameters.script, customDefaults: ['com.sap.piper/pipeline/stageOrdinals.yml'].plus(parameters.customDefaults ?: [])
                }
            }
            stage('Build') {
                steps {
                    cia4autoUiPiperPipelineStageBuild script: parameters.script
                }
            }
            /*stage('Confirm') {
                agent none
                when {allOf {expression { env.BRANCH_NAME ==~ parameters.script.commonPipelineEnvironment.getStepConfiguration('', '').productiveBranch }; anyOf {expression {return (currentBuild.result == 'UNSTABLE')}; expression {return parameters.script.commonPipelineEnvironment.getStepConfiguration('piperInitRunStageConfiguration', env.STAGE_NAME).manualConfirmation}}}}
                steps {
                    piperPipelineStageConfirm script: parameters.script
                }
            }*/
            stage('Promote-Build-Artifact') {
                when{ 
					expression { git_commit_message.contains(git_valid_message.trim()) }
				}
				steps {
					withCredentials([usernameColonPassword(credentialsId: 'GITHUB_CREDS', variable: 'github_credential')]) {
						script{
							node{
							    deleteDir()
								unstash(name: 'DIST')
								check_if_branch_exists = sh (
																script: """ set +x 
																			git ls-remote --heads https://$github_credential@github.com/\"${org}\"/\"${target_repo}\".git $sprint_number
																		""",
																returnStdout: true
															).trim()
								if(check_if_branch_exists.equals("")){
									sh script: """
										git init
										git config user.name "${git_commit_author}"
										git config user.email "${git_commit_email}"
										git add -f dist
										git commit -m "adding the dist directory"
										git push https://$github_credential@github.com/\"$org\"/\"$target_repo\".git master:${sprint_number}"""
								}
								else{
								    sh script: """
										git clone --single-branch --branch ${sprint_number} https://$github_credential@github.com/\"$org\"/\"$target_repo\".git
										cd $target_repo
										rm -rf dist
										cp -r ../dist .
										git config user.name "${git_commit_author}"
										git config user.email "${git_commit_email}"
										git add -f dist
										git commit -m "updating the dist directory"
										git push https://$github_credential@github.com/\"$org\"/\"$target_repo\".git ${sprint_number}"""
								}
							}
						}
					}
				}
            }
        }
        post {
            /* https://jenkins.io/doc/book/pipeline/syntax/#post */
            success {
			    buildSetResult(currentBuild)
			}
            aborted {buildSetResult(currentBuild, 'ABORTED')}
            failure {
				buildSetResult(currentBuild, 'FAILURE')
			    emailext body: """Hello,</br></br>
                            ${env.JOB_NAME} - Build # $BUILD_NUMBER ${currentBuild.currentResult}</br></br>
                            Please check console output <a href='$BUILD_URL'>here</a> to view the details.</br></br></br>
                            Regards,</br>
                            CIA4Auto DevOps Team""", subject: "Attention Required: ${env.JOB_NAME}- Jenkins Build ${currentBuild.currentResult}", to: "${emailTo}"
			}
            unstable {buildSetResult(currentBuild, 'UNSTABLE')}
            cleanup {
                piperPipelineStagePost script: parameters.script
            }
        }
    }
}
