 pipeline {
	agent {
        kubernetes {
            label 'centos7-6gb'
        }
    }
    
	parameters {
        choice choices: ['ci', 'integration', 'milestone', 'release'],
            description: 'Sets the build type. CI builds are used for testing reasons only; integration builds are periodic builds; milestones are considered more-or-less stable, while releases are Eclipse.org releases with a corresponding release review.',
            name: 'BUILD_TYPE'
        string defaultValue: '',
            description: 'If set, the tycho-versions plugin is used to update the versions of the Maven plugins before deployment for graduated plugins',
            name: 'RELEASE_VERSION'
        string defaultValue: '',
            description: 'If set, the tycho-versions plugin is used to update the versions of the incubating Maven plugins before deployment for graduated plugins',
            name: 'INCUBATION_VERSION'
	}
	options {
		buildDiscarder(logRotator(numToKeepStr: '5'))
        timeout(120 /*minutes*/) 
	}
	environment {
	   VERSION_MAVEN_PARAMETER = " ${params.RELEASE_VERSION ? '-Drepository.version=' + params.RELEASE_VERSION : ''} "
	   SIGN_BUILD_PARAMETER = " ${params.BUILD_TYPE == 'ci' ? '' : '-Dsign.build=true'} "
	   SONAR_BRANCH_PARAMETERS = "${env.CHANGE_ID} ? -Dsonar.pullrequest.branch=${env.BRANCH_NAME} -Dsonar.pullrequest.key=${env.CHANGE_ID} -Dsonar.pullrequest.base=${env.CHANGE_TARGET} : -Dsonar.branch.name=${env.BRANCH_NAME}"
	}
	
	tools {
        maven 'apache-maven-3.9.6'
        jdk 'openjdk-jdk17-latest' // Java 11 is selected using Maven toolchains
    }

	stages {
		stage('Maven Bootstrap') { 
			steps {
				sh "mvn -B -f releng/org.eclipse.viatra.parent.core/pom.xml ${SIGN_BUILD_PARAMETER} -Dmaven.repo.local=$WORKSPACE/.repository clean install"
			}
		}
		stage('Full build') { 
			steps {
                xvnc {
                    withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONARCLOUD_TOKEN')]) {
                        withSonarQubeEnv('SonarCloud.io') {
                            sh 'mvn -B -f releng/org.eclipse.viatra.parent.all/pom.xml $SIGN_BUILD_PARAMETER $VERSION_MAVEN_PARAMETER -Dmaven.repo.local=$WORKSPACE/.repository -Dmaven.test.failure.ignore=true -DrunUITests=true clean install '
                            + 'org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594:sonar -Dsonar.projectKey=eclipse-viatra_org.eclipse.viatra -Dsonar.organization=eclipse-viatra -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.token=$SONARCLOUD_TOKEN '
                            + SONAR_BRANCH_PARAMETERS
                        }
                    }
                }
            }
		}
		stage('download.eclipse.org') {
		  when {expression { return params.BUILD_TYPE != "ci" }}
		  steps {
		      sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
		          sh '''
                        export PROJECT=$WORKSPACE/releng/org.eclipse.viatra.update
                        export WORK_DIR=$PROJECT/work
                        export BUILD_DIR=/home/data/httpd/download.eclipse.org/viatra/updates/$BUILD_TYPE
                        
                        export VERSION=$(<$PROJECT/target/classes/version)
                        mkdir $WORK_DIR
                        
                        # Upload repository to download.eclipse.org
                        ssh -o BatchMode=yes genie.viatra@projects-storage.eclipse.org mkdir $BUILD_DIR/$VERSION
                        scp -o BatchMode=yes -r $PROJECT/target/repository/* genie.viatra@projects-storage.eclipse.org:$BUILD_DIR/$VERSION
                        # Upload latest repository
                        scp -o BatchMode=yes -r $PROJECT/target/classes/latest genie.viatra@projects-storage.eclipse.org:$BUILD_DIR
		          '''
		      }
		  }
		}
        stage('Maven Deploy') {
            when {branch "master"} 
			steps {
                script {
                    if (params.RELEASE_VERSION) {
                        sh "mvn -B -f releng/org.eclipse.viatra.parent.all/pom.xml -Dmaven.repo.local=$WORKSPACE/.repository -DnewVersion=$RELEASE_VERSION org.eclipse.tycho:tycho-versions-plugin:set-version"
                        sh "mvn -B -f releng/org.eclipse.viatra.parent.core/pom.xml ${SIGN_BUILD_PARAMETER} -Dmaven.repo.local=$WORKSPACE/.repository clean install"
                    }
                    if (params.INCUBATION_VERSION) {
                        sh "mvn -B -f releng/org.eclipse.viatra.parent.incubation.maven/pom.xml -Dmaven.repo.local=$WORKSPACE/.repository -DnewVersion=$INCUBATION_VERSION org.eclipse.tycho:tycho-versions-plugin:set-version"
                        sh "mvn -B -f releng/org.eclipse.viatra.parent.incubation.maven/pom.xml ${SIGN_BUILD_PARAMETER} -Dmaven.repo.local=$WORKSPACE/.repository clean install"
                    }
                }
                
				sh "mvn -B -f releng/org.eclipse.viatra.parent.core/pom.xml ${SIGN_BUILD_PARAMETER} -Dmaven.repo.local=$WORKSPACE/.repository deploy --fail-never"
                sh "mvn -B -f releng/org.eclipse.viatra.parent.incubation.maven/pom.xml ${SIGN_BUILD_PARAMETER} -Dmaven.repo.local=$WORKSPACE/.repository deploy --fail-never"
			}
		}
	}

	post {
		always {
			archiveArtifacts artifacts: 'releng/org.eclipse.viatra.update/target/repository/**'
            archiveArtifacts artifacts: 'releng/org.eclipse.viatra.update/target/org.eclipse.viatra.update-*.zip'
            archiveArtifacts artifacts: 'query/tests/org.eclipse.viatra.query.rcptt/org.eclipse.viatra.query.rcptt.tests/target/results/**'
            archiveArtifacts artifacts: 'releng/org.eclipse.viatra.docs/target/reference/api/**'
            archiveArtifacts artifacts: 'query/tests/org.eclipse.viatra.query.runtime.cps.tests/BasicCpsTest_coverage.html'
            javadoc javadocDir: 'releng/org.eclipse.viatra.docs/target/reference/api', keepAll: false
			junit testResults: '**/tests/**/target/surefire-reports/*.xml'
		}
    }
}
