package(
    default_visibility = ["//java/google/registry:registry_project"],
)

licenses(["notice"])  # Apache 2.0

# Package up common tools web files
java_library(
    name = "common_tools_webinf",
    resources = glob(["WEB-INF/*"]),
)

# Build common tools war file
genrule(
    name = "common_tools_war",
    tools = [
        "//java/google/registry/module/tools:tools_jar_deploy.jar",
        "//java/google/registry/ui:runfiles_jar",
        "//java/google/registry/ui:assets_recursive_jar",
        ":common_tools_webinf",
        "@appengine_api_sdk//jar",
        "@local_jdk//:bin/jar",
        "@local_jdk//:jdk-lib",
        "@local_jdk//:jre-default",
    ],
    outs = ["common_tools.war"],
    cmd = " && ".join([
        "JAR=$$(pwd)/$(location @local_jdk//:bin/jar)",
        "IN=$$(pwd)/$(SRCS)",
        "OUT=$$(pwd)/$@",
        "BIN=$$(pwd)/$(location //java/google/registry/module/tools:tools_jar_deploy.jar)",
        "WEBINF=$$(pwd)/$(location :common_tools_webinf)",
        "AE_API=$$(pwd)/$(location @appengine_api_sdk//jar)",
        "RUNFILES=$$(pwd)/$(location //java/google/registry/ui:runfiles_jar)",
        "ASSETS=$$(pwd)/$(location //java/google/registry/ui:assets_recursive_jar)",
        "TMP=$$(mktemp -d $${TMPDIR:-/tmp}/common_tools_war.XXXXXXXX)",
        "cd $$TMP",
        "$$JAR -xf $$ASSETS",
        "mv google/registry/ui/assets .",
        "rm -rf com META-INF",
        "mkdir -p assets/js",
        "$$JAR -xf $$RUNFILES",
        "mv google/registry/ui/*.js assets/js/",
        "mv google/registry/ui/css assets/",
        "mv google/registry/ui/html/*.html .",
        "rm -rf com",
        "rm -rf META-INF",
        "$$JAR -xf $$WEBINF",
        "mv google/registry/env/common/tools/WEB-INF .",
        "cd WEB-INF",
        "mkdir lib",
        "cp $$BIN lib",
        "cp $$AE_API lib",
        "cd $$TMP",
        "$$JAR cf common_tools.war .",
        "mv common_tools.war $$OUT",
        "rm -rf $$TMP",
    ])
)

exports_files(glob(["WEB-INF/*"]))
