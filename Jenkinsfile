pipeline {
    agent any

    tools {
        maven 'Maven-3'
        jdk 'JDK-17'
    }

    stages {
        stage('Validate Jira Ticket in Branch Name') {
            steps {
                script {
                    def branchName = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
                    echo "Validating branch/PR name: ${branchName}"

                    def jiraRegex = /^(feature|bugfix|hotfix)\/[A-Z]+-\d+-.+$/

                    if (!(branchName ==~ jiraRegex)) {
                        currentBuild.result = 'FAILURE'
                        error("❌ BUILD FAILED: Branch name '${branchName}' does not comply with standards! Must follow: feature/PROJ-123-short-description")
                    } else {
                        echo "✅ Jira ticket validation passed for branch: ${branchName}"
                    }
                }
            }
        }

        stage('Build Backend (Maven)') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }
    }
}