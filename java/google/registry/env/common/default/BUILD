package(
    default_visibility = ["//java/google/registry:registry_project"],
)

licenses(["notice"])  # Apache 2.0


# Package up common frontend web files
java_library(
    name = "common_default_webinf",
    resources = glob(["WEB-INF/*"]),
)

# Build common default war file
genrule(
    name = "common_default_war",
    tools = [
        "//java/google/registry/module/frontend:frontend_jar_deploy.jar",
        "//java/google/registry/ui:runfiles_jar",
        "//java/google/registry/ui:assets_recursive_jar",
        ":common_default_webinf",
        "@appengine_api_sdk//jar",
        "@local_jdk//:bin/jar",
        "@local_jdk//:jdk-lib",
        "@local_jdk//:jre-default",
    ],
    outs = ["common_default.war"],
    cmd = " && ".join([
        "JAR=$$(pwd)/$(location @local_jdk//:bin/jar)",
        "IN=$$(pwd)/$(SRCS)",
        "OUT=$$(pwd)/$@",
        "BIN=$$(pwd)/$(location //java/google/registry/module/frontend:frontend_jar_deploy.jar)",
        "WEBINF=$$(pwd)/$(location :common_default_webinf)",
        "AE_API=$$(pwd)/$(location @appengine_api_sdk//jar)",
        "RUNFILES=$$(pwd)/$(location //java/google/registry/ui:runfiles_jar)",
        "ASSETS=$$(pwd)/$(location //java/google/registry/ui:assets_recursive_jar)",
        "TMP=$$(mktemp -d $${TMPDIR:-/tmp}/common_default_war.XXXXXXXX)",
        "cd $$TMP",
        "$$JAR -xf $$ASSETS",
        "mv google/registry/ui/assets .",
        "rm -rf com META-INF",
        "mkdir -p assets/js",
        "$$JAR -xf $$RUNFILES",
        "mv google/registry/ui/*.js assets/js/",
        "mv google/registry/ui/css assets/",
        "mv google/registry/ui/html/*.html .",
        "rm -rf com META-INF",
        "$$JAR -xf $$WEBINF",
        "mv google/registry/env/common/default/WEB-INF .",
        "cd WEB-INF",
        "mkdir lib",
        "cp $$BIN lib",
        "cp $$AE_API lib",
        "cd $$TMP",
        "$$JAR cf common_default.war .",
        "mv common_default.war $$OUT",
        "rm -rf $$TMP",
    ])
)

# TODO: is this still needed?
exports_files(glob(["WEB-INF/*"]))
