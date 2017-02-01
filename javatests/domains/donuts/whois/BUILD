package(
    default_testonly = 1,
    default_visibility = ["//visibility:public"],
)

licenses(["notice"])  # Apache 2.0

load("//java/com/google/testing/builddefs:GenTestRules.bzl", "GenTestRules")

java_library(
    name = "whois",
    srcs = glob([
        "*.java",
        "**/*.java",
    ]),
    resources = glob(["**/testdata/*.xml"]),
    deps = [
        "@com_google_appengine_api_1_0_sdk",
        "@com_google_appengine_api_labs",
        "@com_google_code_findbugs_jsr305",
        "@com_google_guava",
        "@com_google_truth",
        "@javax_servlet_api",
        "@joda_time",
        "@junit",
        "@org_mockito_all",
        "//java/domains/donuts",
        "//java/google/registry/model",
        "//java/google/registry/whois",
        "//javatests/google/registry/testing",
    ],
)

GenTestRules(
    name = "GeneratedTestRules",
    test_files = glob([
        "*Test.java",
        "*/*Test.java",
    ]),
    deps = [":whois"],
)
