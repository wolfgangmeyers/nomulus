package(
    default_visibility = ["//java/google/registry:registry_project"],
)

licenses(["notice"])  # Apache 2.0


# Combines the ear META-INF files and module war files into a single ear file.
genrule(
    name = "production_ear",
    tools = [
        "//java/google/registry/env/production/default:production_default_war",
        "//java/google/registry/env/production/backend:production_backend_war",
        "//java/google/registry/env/production/tools:production_tools_war",
        "//java/google/registry/env/common:common_ear_metainf",
        "@local_jdk//:bin/jar",
        "@local_jdk//:jdk-lib",
        "@local_jdk//:jre-default",
    ],
    outs = ["production_ear.ear"],
    cmd = " && ".join([
        "JAR=$$(pwd)/$(location @local_jdk//:bin/jar)",
        "OUT=$$(pwd)/$@",
        "DEFAULT_WAR=$$(pwd)/$(location //java/google/registry/env/production/default:production_default_war)",
        "BACKEND_WAR=$$(pwd)/$(location //java/google/registry/env/production/backend:production_backend_war)",
        "TOOLS_WAR=$$(pwd)/$(location //java/google/registry/env/production/tools:production_tools_war)",
        "METAINF=$$(pwd)/$(location //java/google/registry/env/common:common_ear_metainf)",
        "TMP=$$(mktemp -d $${TMPDIR:-/tmp}/production_ear.XXXXXXXX)",
        "cd $$TMP",
        "$$JAR -xf $$METAINF",
        "rm -rf META-INF",
        "mv google/registry/env/common/META-INF .",
        "rm -rf com",
        "mkdir default",
        "cd default",
        "$$JAR -xf $$DEFAULT_WAR",
        "cd $$TMP",
        "mkdir backend",
        "cd backend",
        "$$JAR -xf $$BACKEND_WAR",
        "cd $$TMP",
        "mkdir tools",
        "cd tools",
        "$$JAR -xf $$TOOLS_WAR",
        "cd $$TMP",
        "$$JAR -cf production_ear.ear .",
        "cp production_ear.ear $$OUT",
        "rm -rf $$TMP",
    ]),
)
