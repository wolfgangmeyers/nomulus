workspace(name = "domain_registry")

load("//java/google/registry:repositories.bzl", "domain_registry_repositories")

domain_registry_repositories()

git_repository(
    name = "io_bazel_rules_closure",
    remote = "https://github.com/donutsinc/rules_closure.git",
    commit = "271f7b7e340a5313c82b3ecbfd035834a1b46153",
)

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_repositories")

closure_repositories(
    omit_gson = True,
    omit_guava = True,
    omit_icu4j = True,
    omit_jsr305 = True,
    omit_jsr330_inject = True,
)
