node {
    stage 'Checkout'
    checkout scm

    stage 'Build'
    sh './gradlew clean build -x test'

    stage 'Test'
    sh './gradlew clean test'

    // Publish the results of the tests
    junit 'build/test-results/*.xml'

    def currentBranch = sh(script: "git show -s --pretty=%d HEAD", returnStdout: true)
    println "Current Branch: $currentBranch"

    // If we are on the 'master' branch release to S3 bucket
    if (currentBranch.contains('origin/master')) {
        stage 'Release'
        // Jenkins works in a detached state. Reattach back to master
        sh 'git checkout -b origin/master'
        sh './gradlew release -x check'
    }
}
