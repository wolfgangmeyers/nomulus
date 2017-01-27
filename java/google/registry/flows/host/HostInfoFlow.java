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

package google.registry.flows.host;

import static google.registry.flows.FlowUtils.validateClientIsLoggedIn;
import static google.registry.flows.ResourceFlowUtils.loadAndVerifyExistence;
import static google.registry.flows.host.HostFlowUtils.validateHostName;
import static google.registry.model.EppResourceUtils.isLinked;
import static google.registry.util.CollectionUtils.difference;

import com.google.common.collect.ImmutableSet;
import com.googlecode.objectify.Key;
import google.registry.flows.EppException;
import google.registry.flows.ExtensionManager;
import google.registry.flows.Flow;
import google.registry.flows.FlowModule.ClientId;
import google.registry.flows.FlowModule.TargetId;
import google.registry.model.eppcommon.StatusValue;
import google.registry.model.eppoutput.EppResponse;
import google.registry.model.host.HostInfoData;
import google.registry.model.host.HostResource;
import google.registry.util.Clock;
import javax.inject.Inject;
import org.joda.time.DateTime;

/**
 * An EPP flow that returns information about a host.
 *
 * <p>The returned information included IP addresses, if any, and details of the host's most recent
 * transfer if it has ever been transferred. Any registrar can see the information for any host.
 *
 * @error {@link google.registry.flows.ResourceFlowUtils.ResourceDoesNotExistException}
 * @error {@link HostFlowUtils.HostNameNotLowerCaseException}
 * @error {@link HostFlowUtils.HostNameNotNormalizedException}
 * @error {@link HostFlowUtils.HostNameNotPunyCodedException}
 */
public final class HostInfoFlow implements Flow {

  @Inject ExtensionManager extensionManager;
  @Inject @ClientId String clientId;
  @Inject @TargetId String targetId;
  @Inject Clock clock;
  @Inject EppResponse.Builder responseBuilder;
  @Inject HostInfoFlow() {}

  @Override
  public EppResponse run() throws EppException {
    extensionManager.validate();  // There are no legal extensions for this flow.
    validateClientIsLoggedIn(clientId);
    validateHostName(targetId);
    DateTime now = clock.nowUtc();
    HostResource host = loadAndVerifyExistence(HostResource.class, targetId, now);
    ImmutableSet.Builder<StatusValue> statusValues = new ImmutableSet.Builder<>();
    // TODO(b/34664935): When LINKED is no longer persisted we won't need to filter it out.
    statusValues.addAll(difference(host.getStatusValues(), StatusValue.LINKED));
    if (isLinked(Key.create(host), now)) {
      statusValues.add(StatusValue.LINKED);
    }
    return responseBuilder
        .setResData(HostInfoData.newBuilder()
            .setFullyQualifiedHostName(host.getFullyQualifiedHostName())
            .setRepoId(host.getRepoId())
            .setStatusValues(statusValues.build())
            .setInetAddresses(host.getInetAddresses())
            .setCurrentSponsorClientId(host.getCurrentSponsorClientId())
            .setCreationClientId(host.getCreationClientId())
            .setCreationTime(host.getCreationTime())
            .setLastEppUpdateClientId(host.getLastEppUpdateClientId())
            .setLastEppUpdateTime(host.getLastEppUpdateTime())
            .setLastTransferTime(host.getLastTransferTime())
            .build())
        .build();
  }
}
