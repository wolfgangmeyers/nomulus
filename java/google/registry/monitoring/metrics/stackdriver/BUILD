package(
    default_visibility = ["//visibility:public"],
)

licenses(["notice"])  # Apache 2.0

java_library(
    name = "stackdriver",
    srcs = glob(["*.java"]),
    deps = [
        "//java/google/registry/monitoring/metrics",
        "@com_google_api_client",
        "@com_google_apis_google_api_services_monitoring",
        "@com_google_appengine_api_1_0_sdk",
        "@com_google_auto_value",
        "@com_google_code_findbugs_jsr305",
        "@com_google_dagger",
        "@com_google_errorprone_error_prone_annotations",
        "@com_google_guava",
        "@com_google_http_client",
        "@com_google_re2j",
        "@joda_time",
    ],
)
