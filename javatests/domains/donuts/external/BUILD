package(
    default_testonly = 1,
    default_visibility = ["//visibility:public"],
)

licenses(["notice"])  # Apache 2.0

load("//java/com/google/testing/builddefs:GenTestRules.bzl", "GenTestRules")

java_library(
    name = "external",
    srcs = glob(["*.java"]),
    resources = ["//java/domains/donuts:env/common/default/WEB-INF/queue.xml"],
    deps = [
        "//java/domains/donuts",
        "//java/google/registry/model",
        "//java/google/registry/request",
        "//java/google/registry/util",
        "//javatests/google/registry/testing",
        "//third_party/java/appengine:appengine-api-testonly",
        "//third_party/java/appengine:appengine-testing",
        "//third_party/java/joda_time",
        "//third_party/java/junit",
        "//third_party/java/mockito",
        "//third_party/java/objectify:objectify-v4_1",
        "//third_party/java/servlet/servlet_api",
        "//third_party/java/truth",
    ],
)

GenTestRules(
    name = "GeneratedTestRules",
    test_files = glob(["*Test.java"]),
    deps = [":external"],
)
