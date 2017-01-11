parallel (
    "Gradle": {
        node('main') {
            stage('Gradle - Checkout') {
                checkout scm
                sh './gradlew --version'
            }

            stage('Gradle - Build') {
                sh './gradlew clean build -x test'
            }

            stage('Gradle - Test') {
                wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                    sh './gradlew clean test'
                }
            }

            // TODO: Enable when Jenkins supports disabling build on CI commit
            //    // If we are on the 'master' branch release to S3 bucket
            //    if (env.BRANCH_NAME == 'master') {
            //        stage('Gradle - Release') {
            //          // Jenkins works in a detached state. Reattach back to master
            //          sh 'git checkout -b master'
            //          sh './gradlew release -x check'
            //        }
            //    }
        }
    },
    "Bazel": {
        node('bazel') {
            stage('Bazel - Checkout') {
                checkout scm
                sh 'bazel --batch info'
            }

            stage('Bazel - Build') {
                sh 'bazel --batch build --javacopt "-source 1.7" --javacopt "-target 1.7" //java/domains/donuts/...'
            }

            stage('Bazel - Test') {
                sh """bazel --batch test --jvmopt "-Djava.security.egd=file:/dev/urandom" //javatests/... \
                    --jobs=2 \
                    --verbose_failures=true \
                    --ram_utilization_factor=10 \
                    --test_keep_going=false \
                    --test_output=errors \
                    --cache_test_results=no \
                    --test_verbose_timeout_warnings=true \
                    --test_sharding_strategy=disabled"""
            }

            if (env.BRANCH_NAME == 'master') {
                stage('Bazel - Deploy') {
                    // TODO: Should this be configurable?
                    sh './build-deploy-artifact.sh war-deploy alpha'
                    withCredentials([file(credentialsId: 'e56f0d0d-9ac6-48a1-91d7-e9f803901d04', variable: 'ALPHA_CREDENTIALS')]) {
                        sh "appcfg.sh --enable_jar_splitting update war-deploy " +
                                "--service_account_json_key_file=$ALPHA_CREDENTIALS"
                    }
                }
            }
        }
    }
)
