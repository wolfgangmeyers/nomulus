node {
    stage 'Checkout'
    checkout scm

    stage 'Build'
    parallel 'gradle build': {
        sh './gradlew --version'
        sh './gradlew clean build -x test'
    }, 'bazel build': {
        sh 'bazel info'
        sh 'bazel build --javacopt "-source 1.7" --javacopt "-target 1.7" //java/domains/donuts/...'
    }

    // Note: Running Gradle and Bazel tests in parallel causes Bazel test timeouts
    stage 'Gradle Test'
    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
        sh './gradlew clean test'
    }

    stage 'Bazel Test'
    // TODO: Migrate/create donuts tests to execute here
    sh """bazel test --javacopt "-source 1.7" --javacopt "-target 1.7" //javatests/google/registry/... \
        --progress_report_interval=1 \
        --jobs=2 \
        --ram_utilization_factor=10 \
        --test_keep_going=false \
        --test_output=errors \
        --cache_test_results=no \
        --test_verbose_timeout_warnings=true \
        --test_sharding_strategy=disabled"""

    // Publish the results of the tests
    stage 'Report'
    junit 'build/test-results/**/*.xml'

    if ("$env.BRANCH_NAME" == 'master') {
        stage 'Deploy'
        // TODO: Should this be configurable?
        sh './build-deploy-artifact.sh war-deploy alpha'
        sh 'appcfg.sh --enable_jar_splitting update war-deploy'
    }


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
