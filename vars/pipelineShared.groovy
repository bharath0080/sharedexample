def call(body) {
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()
	pipeline{
	agent {label config.slave}
	stages{
		stage('checkout'){
			steps{
				dir('source'){
					git url:"${config.Git_url}",credentialsId:"${.config.Git_Credentials}",branch:"${config.Branch_Name}"
				}
			}//steps
		}//stage
		stage('Build Snapshot/Release'){
			steps{
				script{
					configFileprovider([configFile(fileId: 'FIMT_NEXUS_SETTINGS', variable: 'MAVEN_SETTINGS')]){
						if (config.Maven_goal == 'Package')
							sh "mvn -f source/pom.xml clean package -Dmaven.test.skip=true"
						else if(config.Maven_goal == 'Release')
							sh 'mvn -f source/pom.xml -s "${MAVEN_SETTINGS}" --batch-mode release:clean release:prepare release:perform -Dmaven.test.skip=true'
					}//config
				}//script
			}//steps
		}//stage
		stage ('Unittest & Publish'){
			steps{
				sh 'mvn -f source/pom.xml test -Dmaven.test.failure.ignore=false'
				echo "Archiving Unit test results"
				step([$class:'JunitResultAtchiver', testResults: '**/target/surefire-reports/".xml'])
				echo "Publishing Unit test reports"
				sh 'mvn -f source/pom.xml surefire-report:report'
			}//steps
		}//stage
		stage ('code coverage'){
			steps {
				script{
					if (config.code_coverage_tool == 'jacoco' )
					{
						sh 'mvn -f source/pom.xml jacoco:report'
						step([$class : 'JacocoPublisher',
							//execPattern : '**/build/jacoco/*.exec',
							//classPattern : '**/*.class',
							//sourcePattern : '**/src/main/java',
							//minimumBranchCoverage : '45', maximumBranchCoverage: '50',
							//minimumClassCoverage : '45', maximumClassCoverage: '50',
							//minimumComplexityCoverage : '45', maximumComplexityCoverage: '50',
							//minimumInstructionCoverage: '45', maximumInstructionCoverage: '50',
							minimumLineCoverage : '45', maximumLineCoverage: '50',
							//minimumMethodCoverage : '45', maximumMethodCoverage: '50',
							changeBuildstatus: true, deltaBranchCoverage: '1', deltaClassCoverage: '1', deltaComplexityCoverage: '1', deltaInstructionCoverage: '1', deltaLineCoverage: '1', deltaMethodCoverage: '1',
						])
					}//jacoco ends
					else if (config.code_coverage_tool == 'cobertura' )
					{
						sh 'mvn -f source/pom.xml -cobertura:report'
						step([$class: 'CoberturaPublisher', autoUpdateHealth: true, autoUpdateStability: true, coberturaReportFile: 'coverage.xml', maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
					}
					else if (config.code_coverage_tool == 'Scoverage&cobertura' )
					{
						sh 'mvn -f source/pom.xml -DScoverage:report'
						step([$class: 'CoberturaPublisher', autoUpdateHealth: true, autoUpdateStability: true, coberturaReportFile: 'coverage.xml', maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
					}
				}//script
			}//steps
		}//stage
		stage('sonar'){
			steps{
				withSonarQubeEnv('Enterprise Sonar PROD'){
					sh 'mvn -f source/pom.xml -DSonarqube sonar:sonar'
				}
			}//steps
		}//stage
		stage('TriggerJob'){
			steps{
				dir('source'){
					script{
						def pom = readMavenPom file : 'pom.xml'
						version=pom.version
						build job: 'test', parameters: [string(name:'version',value:version)]
					}
				}
			}//steps
		}//stage
	}//stages
	post{
		always{
			cleanWS deleteDirs: false
		}
		failure{
			emailext(
				body:"Please go to ${BUILD_URL} and verify the build",
				subject:Failed Job '${JOB_NAME}' (${BUILD_NUMBER})
				to: "${config.email}"
			)
		}
	}//post
	}//pipeline
}


