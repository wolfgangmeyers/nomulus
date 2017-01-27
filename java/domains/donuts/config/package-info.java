/**
 * TODO: Move all Donuts configuration into .yaml files.
 *
 * Once Google has finished moving all the configuration into .yaml files we will no
 * longer need to maintain our own ConfigModule. All configuration values will be stored
 * in /domains/donuts/env/{env}/common/WEB-INF/nomulus-config.yaml and read in using
 * {@link google.registry.config.YamlUtils}.
 *
 * Until everything is moved we will need to maintain our own copy of the YamlUtils
 * {@link domains.donuts.config.DonutsYamlUtils}. This is due to the package private method
 * {@link google.registry.config.YamlUtils#getConfigSettings()} which is called from the
 * {@link google.registry.config.RegistryConfig}.
 */
package domains.donuts.config;