// Copyright 2016 The Domain Registry Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package domains.donuts.config;

import static domains.donuts.config.DonutsConfigModule.CONFIG_SETTINGS;

import com.google.common.base.Joiner;
import com.google.common.net.HostAndPort;
import google.registry.config.RegistryConfig;
import google.registry.config.RegistryEnvironment;
import javax.annotation.concurrent.Immutable;
import org.joda.time.Duration;

/** Donuts production {@link RegistryConfig}. */
@Immutable
public final class DonutsRegistryConfig {

  private static final String RESERVED_TERMS_EXPORT_DISCLAIMER = ""
      + "# This list contains reserve terms for the TLD. Other terms may be reserved\n"
      + "# but not included in this list, including terms Donuts Inc. chooses not\n"
      + "# to publish, and terms that ICANN commonly mandates to be reserved. This\n"
      + "# list is subject to change and the most up-to-date source is always to\n"
      + "# check availability directly with the Registry server.\n";


  public static String getProjectId() {
    return CONFIG_SETTINGS.get().appEngine.projectId;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Thirty days makes a sane default, because it's highly unlikely we'll ever need to generate a
   * deposit older than that. And if we do, we could always bring up a separate App Engine instance
   * and replay the commit logs off GCS.
   */
  public static Duration getCommitLogDatastoreRetention() {
    return Duration.standardDays(30);
  }

  public static boolean getTmchCaTestingMode() {
    switch (RegistryEnvironment.get()) {
      case PRODUCTION:
        return false;
      default:
        return true;
    }
  }

  public static HostAndPort getServer() {
    switch (RegistryEnvironment.get()) {
      case LOCAL:
        return HostAndPort.fromParts("localhost", 8080);
      default:
        String host = Joiner.on(".").join("tools", getProjectId(), "appspot.com");
        return HostAndPort.fromParts(host, 443);
    }
  }

  public static int getEppResourceIndexBucketCount() {
    return 997;  // decrease this and the world ends
  }

}
