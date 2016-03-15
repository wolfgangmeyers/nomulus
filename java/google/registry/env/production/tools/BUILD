package(
    default_visibility = ["//java/google/registry:registry_project"],
)

licenses(["notice"])  # Apache 2.0


# packages production tools web files
java_library(
    name = "production_tools_webinf",
    resources = glob(["WEB-INF/*"]),
)

# adds production files to tools war
genrule(
    name = "production_tools_war",
    tools = [
        "//java/com/google/domain/registry/env/common/tools:common_tools_war",
        ":production_tools_webinf",
        "@local_jdk//:bin/jar",
        "@local_jdk//:jdk-lib",
        "@local_jdk//:jre-default",
    ],
    outs = ["production_tools_war.war"],
    cmd = " && ".join([
        "JAR=$$(pwd)/$(location @local_jdk//:bin/jar)",
        "OUT=$$(pwd)/$@",
        "COMMON_WAR=$$(pwd)/$(location //java/com/google/domain/registry/env/common/tools:common_tools_war)",
        "WEBINF=$$(pwd)/$(location :production_tools_webinf)",
        "TMP=$$(mktemp -d $${TMPDIR:-/tmp}/production_tools_war.XXXXXXXX)",
        "cd $$TMP",
        "mkdir war",
        "$$JAR -xf $$WEBINF",
        "mv com/google/domain/registry/env/production/tools/WEB-INF .",
        "cd war",
        "$$JAR -xf $$COMMON_WAR",
        "cp ../WEB-INF/* WEB-INF/",
        "$$JAR -cf production_tools_war.war .",
        "cp production_tools_war.war $$OUT",
        "rm -rf $$TMP",
    ]),
)
