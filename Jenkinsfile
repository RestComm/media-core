#!groovy​
pipeline {
    agent none

    stages {
        stage('Checkout') {
            agent { node('cxs-slave-master') }

            steps {
                echo sh(returnStdout: true, script: 'env')
                configFileProvider([configFile(fileId: '37cb206e-6498-4d8a-9b3d-379cd0ccd99b', targetLocation: 'settings.xml')]) {
                    sh 'mkdir -p ~/.m2 && sed -i "s|@LOCAL_REPO_PATH@|$WORKSPACE/M2_REPO|g" $WORKSPACE/settings.xml && cp $WORKSPACE/settings.xml -f ~/.m2/settings.xml'
                }
                checkout scm
            }

        }

        stage('Build') {
            agent { node('cxs-slave-master') }
            steps {
                configFileProvider([configFile(fileId: '37cb206e-6498-4d8a-9b3d-379cd0ccd99b', targetLocation: 'settings.xml')]) {
                    sh 'mkdir -p ~/.m2 && sed -i "s|@LOCAL_REPO_PATH@|$WORKSPACE/M2_REPO|g" $WORKSPACE/settings.xml && cp $WORKSPACE/settings.xml -f ~/.m2/settings.xml'
                }
                sh "mvn clean install -DskipTests"
            }
        }

        stage('Test') {
            agent { node('cxs-slave-master') }
            steps {
                configFileProvider([configFile(fileId: '37cb206e-6498-4d8a-9b3d-379cd0ccd99b', targetLocation: 'settings.xml')]) {
                    sh 'mkdir -p ~/.m2 && sed -i "s|@LOCAL_REPO_PATH@|$WORKSPACE/M2_REPO|g" $WORKSPACE/settings.xml && cp $WORKSPACE/settings.xml -f ~/.m2/settings.xml'
                }
                sh 'mvn test -Dmaven.test.failure.ignore=true'
                junit testResults: '**/target/surefire-reports/*.xml', testDataPublishers: [[$class: 'StabilityTestDataPublisher']]
            }
        }

        stage('UserApproval') {
            agent none

            steps {
                script {
                    def userInput = input message: 'Waiting for maintainer review', parameters:
                        [choice(name: 'FEATURE_SCOPE', choices: 'fix\nfeat\nbreaking_change', description: 'Release Scope'),
                         text(name: 'COMMIT_MSG', defaultValue: '', description: 'Commit Message')]
                    env.FEATURE_SCOPE = userInput['FEATURE_SCOPE']
                    env.COMMIT_MSG = userInput['COMMIT_MSG']
                }
                milestone 1
            }
        }

        stage('Integration') {
            agent { node('cxs-slave-master') }

            steps {
                lock('media-core-master') {
                    script {
                        // Find feature author
                        env.COMMIT_AUTHOR = sh(script: 'git log -1 --pretty=format:\'%an <%ae>\'', returnStdout: true).trim()

                        // Increment project version according to release scope
                        if(env.FEATURE_SCOPE == 'fix') {
                            sh 'mvn build-helper:parse-version versions:set -DnewVersion=\\${parsedVersion.majorVersion}.\\${parsedVersion.minorVersion}.\\${parsedVersion.nextIncrementalVersion}-SNAPSHOT versions:commit'
                        } else if(env.FEATURE_SCOPE == 'feat') {
                            sh 'mvn build-helper:parse-version versions:set -DnewVersion=\\${parsedVersion.majorVersion}.\\${parsedVersion.nextMinorVersion}.0-SNAPSHOT versions:commit'
                        } else if(env.FEATURE_SCOPE == 'breaking_change') {
                            sh 'mvn build-helper:parse-version versions:set -DnewVersion=\\${parsedVersion.nextMajorVersion}.0.0-SNAPSHOT versions:commit'
                        }

                        sh 'git add *'
                        sh 'git commit -m "Updated project version to $NEXT_VERSION"'

                        // Save project version
                        def pom = readMavenPom file: 'pom.xml'
                        env.NEXT_VERSION = pom.version
                        echo "Updated project version to $NEXT_VERSION"

                        // Merge feature
                        sh 'git checkout master'
                        sh 'git merge --squash origin/$BRANCH_NAME'
                        sh 'git commit -a --author="$COMMIT_AUTHOR" --message="$COMMIT_MSG"'

                        def gitLog = sh(script: 'git log -1 --pretty=format:full', returnStdout: true)
                        echo "${gitLog}"

                        // Push changes
                        withCredentials([usernamePassword(credentialsId: 'CXSGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                            // TODO Push to master branch
                            // Invalidate older builds forcing re-scan of PR
                            // Aims to maintain master healthy and prevent that one PR tramples another
                            milestone 2
                        }
                    }
                }
            }
        }
    }
}
