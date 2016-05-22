package(
    default_visibility = ["//java/google/registry:registry_project"],
)

licenses(["notice"])  # Apache 2.0


# packages production default web files
java_library(
    name = "production_default_webinf",
    resources = glob(["WEB-INF/*"]),
)

# adds production files to default war
genrule(
    name = "production_default_war",
    tools = [
        "//java/google/registry/env/common/default:common_default_war",
        ":production_default_webinf",
        "@local_jdk//:bin/jar",
        "@local_jdk//:jdk-lib",
        "@local_jdk//:jre-default",
    ],
    outs = ["production_default_war.war"],
    cmd = " && ".join([
        "JAR=$$(pwd)/$(location @local_jdk//:bin/jar)",
        "OUT=$$(pwd)/$@",
        "COMMON_WAR=$$(pwd)/$(location //java/google/registry/env/common/default:common_default_war)",
        "WEBINF=$$(pwd)/$(location :production_default_webinf)",
        "TMP=$$(mktemp -d $${TMPDIR:-/tmp}/production_default_war.XXXXXXXX)",
        "cd $$TMP",
        "mkdir war",
        "$$JAR -xf $$WEBINF",
        "mv google/registry/env/production/default/WEB-INF .",
        "cd war",
        "$$JAR -xf $$COMMON_WAR",
        "cp ../WEB-INF/* WEB-INF/",
        "$$JAR -cf production_default_war.war .",
        "cp production_default_war.war $$OUT",
        "rm -rf $$TMP",
    ]),
)
