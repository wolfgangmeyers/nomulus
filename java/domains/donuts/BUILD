package(default_visibility = ["//java/google/registry:registry_project"])

licenses(["notice"])  # Apache 2.0

java_library(
    name = "donuts",
    srcs = glob([
        "*.java",
        "**/*.java",
    ]),
    resources = glob([
        "**/*.asc",
    ]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/net",
        "//java/com/google/common/io",
        "//java/google/registry/keyring/api",
        "//java/google/registry/flows",
        "//java/google/registry/model",
        "//java/google/registry/config",
        "//java/google/registry/request",
        "//third_party/java/appengine:appengine-api",
        "//third_party/java/bouncycastle",
        "//third_party/java/bouncycastle_bcpg",
        "//third_party/java/dagger",
        "//third_party/java/joda_time",
        "//third_party/java/jsr305_annotations",
        "//third_party/java/servlet/servlet_api",
    ],
)
