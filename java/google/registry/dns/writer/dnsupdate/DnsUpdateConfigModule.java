// Copyright 2016 The Nomulus Authors. All Rights Reserved.
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

package google.registry.dns.writer.dnsupdate;

import dagger.Module;
import dagger.Provides;
import google.registry.config.RegistryConfig.Config;
import google.registry.config.RegistryEnvironment;
import org.joda.time.Duration;

/** Dagger module that provides DNS configuration settings. */
@Module
public class DnsUpdateConfigModule {

  /**
   * Host that receives DNS updates from the registry.
   * Usually a "hidden master" for the TLDs.
   */
  @Provides
  @Config("dnsUpdateHost")
  public static String provideDnsUpdateHost(RegistryEnvironment environment) {
    switch (environment) {
      case ALPHA:
        return "ns-master.alpha.hg.team";
      case CRASH:
        return "ns-master.crash.hg.team";
      case PRODUCTION:
        return "ns-master.test.hg.team";
      case QA:
      case SANDBOX:
      default:
        throw new UnsupportedOperationException("not implemented");
    }
  }

  /**
   * Timeout on the socket for DNS update requests.
   */
  @Provides
  @Config("dnsUpdateTimeout")
  public static Duration provideDnsUpdateTimeout() {
    return Duration.standardSeconds(30);
  }

  /**
   * The DNS time-to-live (TTL) for resource records created by the registry.
   */
  @Provides
  @Config("dnsUpdateTimeToLive")
  public static Duration provideDnsUpdateTimeToLive() {
    return Duration.standardHours(24);
  }
}
