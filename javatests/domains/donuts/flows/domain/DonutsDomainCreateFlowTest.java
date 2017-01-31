// Copyright 2016 Donuts Inc. All Rights Reserved.
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

package domains.donuts.flows.domain;

import static com.google.common.truth.Truth.assertThat;
import static domains.donuts.config.DonutsConfigModule.provideDpmlCreateOverridePrice;
import static domains.donuts.config.DonutsConfigModule.provideDpmlTld;
import static google.registry.model.eppoutput.Result.Code.SUCCESS;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistActiveDomain;
import static google.registry.testing.DatastoreHelper.persistReservedList;
import static google.registry.testing.DatastoreHelper.persistResource;

import com.google.appengine.labs.repackaged.com.google.common.collect.ImmutableMap;
import domains.donuts.flows.custom.DonutsDomainCreateFlowCustomLogic.DpmlBlockedException;
import domains.donuts.flows.custom.DonutsDomainCreateFlowCustomLogic.SignedMarksRequiredException;
import google.registry.flows.domain.DomainFlowUtils;
import google.registry.flows.domain.DomainFlowUtils.FeesMismatchException;
import google.registry.model.domain.fee.Fee;
import google.registry.model.domain.fee.FeeTransformResponseExtension;
import google.registry.model.eppoutput.EppOutput;
import google.registry.model.eppoutput.Result;
import google.registry.model.registry.Registry;
import google.registry.model.registry.label.ReservedList;
import java.math.BigDecimal;
import java.util.List;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DonutsDomainCreateFlowTest extends DonutsDomainCreateFlowTestCase {

  private final String dpmlTld = provideDpmlTld();
  private final Money dpmlOverridePrice = provideDpmlCreateOverridePrice();

  @Before
  public void setUp() throws Exception {
    setEppInput("domain_create.xml");
    createTld("tld");
    createTld(dpmlTld);
    persistNetContactsAndHosts();
  }

  @Test
  @Ignore(
      "This test validates internal dpml blocks which will need to be configured and added once "
          + "ALL tlds are ran from Nomulus. Until then the external block will be used.")
  public void testCreate_internal_BlockedByDpml() throws Exception {
    setEppInput("domain_create.xml");
    persistActiveDomain("example." + dpmlTld);
    thrown.expect(DpmlBlockedException.class, "The requested domain name is blocked by DPML");
    runDonutsFlow();
  }

  @Test
  public void testCreate_external_BlockedByDpml() throws Exception {
    setEppInput("domain_create.xml");
    persistExternalDPMLBlock("example");
    thrown.expect(DpmlBlockedException.class, "The requested domain name is blocked by DPML");
    runDonutsFlow();
  }

  @Test
  public void testDpmlOverrideBlockedNoSmd() throws Exception {
    setEppInput("domain_create_dpml_phase_no_smd.xml");
    // TODO: Should this have a different error message?
    thrown.expect(SignedMarksRequiredException.class, "SMD required for DPML block registration");
    runDonutsFlow();
  }

  @Test
  public void testDpmlOverrideBlockedNoFee() throws Exception {
    setEppInput("domain_create_dpml_phase_encoded_smd_no_fee.xml");
    persistExternalDPMLBlock("test-validate");
    thrown.expect(
        getNestedPrivateThrowableClass(
            "FeesRequiredForNonFreeOperationException", DomainFlowUtils.class),
        "Fees must be explicitly acknowledged when performing an operation which is not free.");
    runDonutsFlow();
  }

  @Test
  public void testDpmlOverrideBlockedIncorrectPrice() throws Exception {
    setEppInput(
        "domain_create_dpml_phase_encoded_smd_fee.xml",
        ImmutableMap.of(
            "FEE_VERSION",
            "0.12",
            "DPML_OVERRIDE_PRICE",
            dpmlOverridePrice.plus(1.00).getAmount().toString()));
    persistExternalDPMLBlock("test-validate");
    thrown.expect(
        FeesMismatchException.class,
        "The fees passed in the transform command do not match the expected total");
    runDonutsFlow();
  }

  @Test
  public void testDpmlOverride() throws Exception {
    setEppInput(
        "domain_create_dpml_phase_encoded_smd_fee.xml",
        ImmutableMap.of(
            "FEE_VERSION",
            "0.12",
            "DPML_OVERRIDE_PRICE",
            dpmlOverridePrice.getAmount().toString()));
    persistExternalDPMLBlock("test-validate");
    final Result result = runDonutsFlow().getResponse().getResult();
    assertThat(result).isEqualTo(Result.create(SUCCESS));
  }

  @Test
  public void testReserved_ShouldThrowReservedError() throws Exception {
    setEppInput("domain_create.xml");
    persistExternalDPMLBlock("example");
    final ReservedList reservedList =
        persistReservedList("tld", "example,FULLY_BLOCKED");
    final Registry tld = Registry.get("tld").asBuilder().setReservedLists(reservedList).build();
    persistResource(tld);
    thrown.expect(
        getNestedPrivateThrowableClass(
            "DomainReservedException", DomainFlowUtils.class),
        "example.tld is a reserved domain");
    runDonutsFlow();
  }

  @Test
  public void testUnreserved_ShouldNotThrowReservedError() throws Exception {
    persistExternalDPMLBlock("example");
    final ReservedList reservedList =
        persistReservedList("tld", "example,UNRESERVED");
    final Registry tld = Registry.get("tld").asBuilder().setReservedLists(reservedList).build();
    persistResource(tld);
    thrown.expect(DpmlBlockedException.class,
        "The requested domain name is blocked by DPML");
    runDonutsFlow();
  }

  @Test
  public void testPremium_ShouldNotThrowError() throws Exception {
    setEppInput("domain_create_premium_domain.xml", ImmutableMap.of("FEE_VERSION", "0.12"));

    // 'rich.tld' is created by default in `create(tld)` and can be found in
    // DatastoreHelper.java specifically this file `default_premium_list_testdata.csv`
    persistExternalDPMLBlock("rich");

    final EppOutput result = runDonutsFlow();
    final FeeTransformResponseExtension extension =
        (FeeTransformResponseExtension) result.getResponse().getExtensions().get(0);
    assertThat(result.getResponse().getResult()).isEqualTo(Result.create(SUCCESS));

    final List<Fee> fees = getField(FeeTransformResponseExtension.class, "fees", extension);
    assertThat(fees.get(0).getCost()).isEqualTo(new BigDecimal("100.00"));
  }
}
