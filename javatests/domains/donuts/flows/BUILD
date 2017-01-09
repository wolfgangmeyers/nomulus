package(
    default_testonly = 1,
    default_visibility = ["//visibility:public"],
)

licenses(["notice"])  # Apache 2.0

load("//java/com/google/testing/builddefs:GenTestRules.bzl", "GenTestRules")

java_library(
    name = "flows",
    srcs = glob([
        "*.java",
        "**/*.java",
    ]),
    resources = glob(["**/testdata/*.xml"]),
    deps = [
        "//java/domains/donuts",
        "//java/google/registry/config",
        "//java/google/registry/dns",
        "//java/google/registry/flows",
        "//java/google/registry/model",
        "//java/google/registry/monitoring/whitebox",
        "//java/google/registry/request",
        "//java/google/registry/util",
        "//javatests/google/registry/flows",
        "//javatests/google/registry/testing",
        "@com_google_appengine_api_1_0_sdk",
        "@com_google_appengine_api_labs",
        "@com_google_code_findbugs_jsr305",
        "@com_google_dagger",
        "@com_google_guava",
        "@com_google_truth",
        "@javax_servlet_api",
        "@joda_time",
        "@junit",
        "@org_mockito_all",
    ],
)

GenTestRules(
    name = "GeneratedTestRules",
    test_files = glob([
        "*Test.java",
        "*/*Test.java",
    ]),
    deps = [":flows"],
)
