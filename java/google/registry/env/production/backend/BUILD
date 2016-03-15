package(
    default_visibility = ["//java/google/registry:registry_project"],
)

licenses(["notice"])  # Apache 2.0


# packages backend production web files
java_library(
    name = "production_backend_webinf",
    resources = glob(["WEB-INF/*"]),
)

# adds production files to backend war
# XXX: Requires extra jdk dependencies - see https://github.com/bazelbuild/bazel/issues/1051
genrule(
    name = "production_backend_war",
    tools = [
        "//java/com/google/domain/registry/env/common/backend:common_backend_war",
        ":production_backend_webinf",
        "@local_jdk//:bin/jar",
        "@local_jdk//:jdk-lib",
        "@local_jdk//:jre-default",
    ],
    outs = ["production_backend_war.war"],
    cmd = " && ".join([
        "JAR=$$(pwd)/$(location @local_jdk//:bin/jar)",
        "OUT=$$(pwd)/$@",
        "COMMON_WAR=$$(pwd)/$(location //java/com/google/domain/registry/env/common/backend:common_backend_war)",
        "WEBINF=$$(pwd)/$(location :production_backend_webinf)",
        "TMP=$$(mktemp -d $${TMPDIR:-/tmp}/production_backend_war.XXXXXXXX)",
        "cd $$TMP",
        "mkdir war",
        "$$JAR -xf $$WEBINF",
        "mv com/google/domain/registry/env/production/backend/WEB-INF .",
        "cd war",
        "$$JAR -xf $$COMMON_WAR",
        "cp ../WEB-INF/* WEB-INF/",
        "$$JAR -cf production_backend_war.war .",
        "cp production_backend_war.war $$OUT",
        "rm -rf $$TMP",
    ]),
)
