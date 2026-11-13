pipeline {
    agent any
    
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
        
        stage('Build & Unit Test') {
            steps {
                sh 'mvn clean test-compile'
            }
        }
        
        stage('Deploy to Dev & Run Smoke Tests') {
            steps {
                echo "Deploying application stack for Dev environment..."
                sh 'docker compose up -d --build'
                
                echo "Running Smoke Test Suite..."
                sh 'mvn test -Dgroups=smoke'
            }
        }
        
        stage('Deploy to Test & Run Regression Tests') {
            steps {
                echo "Deploying Test Environment on port 8082..."
                sh 'docker compose -f docker-compose.test.yml up -d --build'
                
                echo "Running Regression Test Suite against Test Environment..."
                sh 'mvn test -Dgroups=regression'
            }
        }
        
        stage('Production Approval & Deployment') {
            when {
                branch 'main'
            }
            steps {
                input message: "Approve deployment to Production (Port 8083)?", ok: "Deploy"
                
                echo "Deploying Production Environment..."
                sh 'docker compose -f docker-compose.prod.yml up -d --build'
                echo "✅ Successfully deployed to Production!"
            }
        }
    }
    
    post {
        always {
            echo "Pipeline execution finished."
        }
        success {
            echo "🎉 CI/CD Pipeline completed successfully across all environments!"
        }
        failure {
            echo "❌ Pipeline failed during validation, build, or test execution."
        }
    }
}