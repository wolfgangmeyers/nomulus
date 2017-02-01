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

package domains.donuts.whois;

import static domains.donuts.config.DonutsConfigModule.provideDpmlLookup;

import com.google.common.base.Optional;
import com.google.common.net.InternetDomainName;
import google.registry.whois.DomainLookupCommand;
import google.registry.whois.WhoisResponse;
import org.joda.time.DateTime;

/** Represents a WHOIS lookup on a domain name (i.e. SLD) possibly containing a DPML block. */
public class DonutsDomainLookupCommand extends DomainLookupCommand {

  public DonutsDomainLookupCommand(InternetDomainName domainName) {
    super(domainName);
  }

  @Override
  protected Optional<WhoisResponse> getResponse(InternetDomainName domainName, DateTime now) {
    final Optional<WhoisResponse> response = super.getResponse(domainName, now);
    if (!response.isPresent() && provideDpmlLookup().isBlocked(domainName, now)) {
      return Optional.of((WhoisResponse) new DpmlWhoisResponse(now));
    }
    return response;
  }
}
