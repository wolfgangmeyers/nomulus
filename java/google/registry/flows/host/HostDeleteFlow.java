// Copyright 2017 The Nomulus Authors. All Rights Reserved.
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
import static google.registry.flows.ResourceFlowUtils.failfastForAsyncDelete;
import static google.registry.flows.ResourceFlowUtils.loadAndVerifyExistence;
import static google.registry.flows.ResourceFlowUtils.verifyNoDisallowedStatuses;
import static google.registry.flows.ResourceFlowUtils.verifyResourceOwnership;
import static google.registry.flows.host.HostFlowUtils.validateHostName;
import static google.registry.model.eppoutput.Result.Code.SUCCESS_WITH_ACTION_PENDING;
import static google.registry.model.ofy.ObjectifyService.ofy;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.googlecode.objectify.Key;
import google.registry.flows.EppException;
import google.registry.flows.ExtensionManager;
import google.registry.flows.FlowModule.ClientId;
import google.registry.flows.FlowModule.Superuser;
import google.registry.flows.FlowModule.TargetId;
import google.registry.flows.TransactionalFlow;
import google.registry.flows.async.AsyncFlowEnqueuer;
import google.registry.model.domain.DomainBase;
import google.registry.model.domain.metadata.MetadataExtension;
import google.registry.model.eppcommon.StatusValue;
import google.registry.model.eppoutput.EppResponse;
import google.registry.model.host.HostResource;
import google.registry.model.reporting.HistoryEntry;
import javax.inject.Inject;
import org.joda.time.DateTime;

/**
 * An EPP flow that deletes a host.
 *
 * <p>Hosts that are in use by any domain cannot be deleted. The flow may return immediately if a
 * quick smoke check determines that deletion is impossible due to an existing reference. However, a
 * successful delete will always be asynchronous, as all existing domains must be checked for
 * references to the host before the deletion is allowed to proceed. A poll message will be written
 * with the success or failure message when the process is complete.
 *
 * @error {@link google.registry.flows.ResourceFlowUtils.ResourceDoesNotExistException}
 * @error {@link google.registry.flows.ResourceFlowUtils.ResourceNotOwnedException}
 * @error {@link google.registry.flows.exceptions.ResourceStatusProhibitsOperationException}
 * @error {@link google.registry.flows.exceptions.ResourceToDeleteIsReferencedException}
 * @error {@link HostFlowUtils.HostNameNotLowerCaseException}
 * @error {@link HostFlowUtils.HostNameNotNormalizedException}
 * @error {@link HostFlowUtils.HostNameNotPunyCodedException}
 */
public final class HostDeleteFlow implements TransactionalFlow {

  private static final ImmutableSet<StatusValue> DISALLOWED_STATUSES = ImmutableSet.of(
      StatusValue.CLIENT_DELETE_PROHIBITED,
      StatusValue.PENDING_DELETE,
      StatusValue.SERVER_DELETE_PROHIBITED);

  private static final Function<DomainBase, ImmutableSet<?>> GET_NAMESERVERS =
      new Function<DomainBase, ImmutableSet<?>>() {
        @Override
        public ImmutableSet<?> apply(DomainBase domain) {
          return domain.getNameservers();
        }};

  @Inject ExtensionManager extensionManager;
  @Inject @ClientId String clientId;
  @Inject @TargetId String targetId;
  @Inject @Superuser boolean isSuperuser;
  @Inject HistoryEntry.Builder historyBuilder;
  @Inject AsyncFlowEnqueuer asyncFlowEnqueuer;
  @Inject EppResponse.Builder responseBuilder;
  @Inject HostDeleteFlow() {}

  @Override
  public final EppResponse run() throws EppException {
    extensionManager.register(MetadataExtension.class);
    extensionManager.validate();
    validateClientIsLoggedIn(clientId);
    DateTime now = ofy().getTransactionTime();
    validateHostName(targetId);
    failfastForAsyncDelete(targetId, now, HostResource.class, GET_NAMESERVERS);
    HostResource existingHost = loadAndVerifyExistence(HostResource.class, targetId, now);
    verifyNoDisallowedStatuses(existingHost, DISALLOWED_STATUSES);
    if (!isSuperuser) {
      verifyResourceOwnership(clientId, existingHost);
    }
    asyncFlowEnqueuer.enqueueAsyncDelete(existingHost, clientId, isSuperuser);
    HostResource newHost =
        existingHost.asBuilder().addStatusValue(StatusValue.PENDING_DELETE).build();
    historyBuilder
        .setType(HistoryEntry.Type.HOST_PENDING_DELETE)
        .setModificationTime(now)
        .setParent(Key.create(existingHost));
    ofy().save().<Object>entities(newHost, historyBuilder.build());
    return responseBuilder.setResultFromCode(SUCCESS_WITH_ACTION_PENDING).build();
  }
}
