//def call(body) {
def call(Map config) {
	/*def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()*/
	pipeline{
	agent {label config.slave}
	stages{
		stage('checkout'){
			steps{
				dir('source'){
					//git url:"${config.Git_url}",credentialsId:"${.config.Git_Credentials}",branch:"${config.Branch_Name}"
					println config.Git_url
				}
			}//steps
		}//stage
		stage('Build Snapshot/Release'){
			steps{
				script{
					//configFileprovider([configFile(fileId: 'FIMT_NEXUS_SETTINGS', variable: 'MAVEN_SETTINGS')]){
						if (config.Maven_goal == 'Package')
							//sh "mvn -f source/pom.xml clean package -Dmaven.test.skip=true"
						println config.Maven_goal
						else if(config.Maven_goal == 'Release')
							//sh 'mvn -f source/pom.xml -s "${MAVEN_SETTINGS}" --batch-mode release:clean release:prepare release:perform -Dmaven.test.skip=true'
						println config.Maven_goal
					//}//config
				}//script
			}//steps
		}//stage
		stage ('Unittest & Publish'){
			steps{
				sh "echo unittest"
			}//steps
		}//stage
		stage ('code coverage'){
			steps {
				script{
					if (config.code_coverage_tool == 'jacoco' )
					{
						sh 'echo jacoco'
						
					}//jacoco ends
					else if (config.code_coverage_tool == 'cobertura' )
					{
						sh "echo cobertura"
					}
					else if (config.code_coverage_tool == 'Scoverage&cobertura' )
					{
						sh "echo Scoverage"
					}
				}//script
			}//steps
		}//stage
		stage('sonar'){
			steps{
				sh "echo sonar"
			}//steps
		}//stage
		
	}//stages
	post{
		always{
			cleanWs deleteDirs: false
		}
		failure{
			emailext(
				body:"Please go to ${BUILD_URL} and verify the build",
				subject:"Failed Job ${JOB_NAME} ${BUILD_NUMBER}",
				to: "${config.email}"
			)
		}
	}//post
	}//pipeline
}


