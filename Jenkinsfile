node("b2-bazel") {
    def java7 = "PATH+JAVA=${tool 'java7'}/bin"

    stage 'Checkout'
    checkout scm

    stage 'Build'
    parallel 'gradle build': {
        withEnv(["${java7}"]) {
            sh 'java -version'
            sh './gradlew clean build -x test'
        }
    }, 'bazel build': {
        sh 'bazel build //java/domains/donuts/...'
    }

    // Note: Running Gradle and Bazel tests in parallel causes Bazel test timeouts
    stage 'Gradle Test'
    withEnv(["${java7}"]) {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
            sh './gradlew clean test'
        }
    }

    stage 'Bazel Test'
    // TODO: Migrate/create donuts tests to execute here
    sh """bazel test //javatests/google/registry/... \
        --local_resources=4000,2,1 \
        --test_output=errors \
        --test_summary=detailed \
        --cache_test_results=no \
        --test_verbose_timeout_warnings=true \
        --test_sharding_strategy=disabled"""

    // Publish the results of the tests
    stage 'Report'
    junit 'build/test-results/**/*.xml'

// TODO: Enable when Jenkins supports disabling build on CI commit
//    def currentBranch = sh(script: "git show -s --pretty=%d HEAD", returnStdout: true)
//    println "Current Branch: $currentBranch"
//
//    // If we are on the 'master' branch release to S3 bucket
//    if (currentBranch.contains('origin/master')) {
//        stage 'Release'
//        // Jenkins works in a detached state. Reattach back to master
//        sh 'git checkout -b master'
//        sh './gradlew release -x check'
//    }
}
