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
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistActiveDomain;
import static google.registry.testing.DatastoreHelper.persistReservedList;
import static google.registry.testing.DatastoreHelper.persistResource;

import com.google.appengine.labs.repackaged.com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Work;
import google.registry.model.domain.fee.FeeCheckResponseExtension;
import google.registry.model.domain.fee.FeeCheckResponseExtensionItem;
import google.registry.model.eppoutput.CheckData;
import google.registry.model.eppoutput.EppResponse;
import google.registry.model.registry.Registry;
import google.registry.model.registry.label.ReservedList;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DonutsDomainCheckFlowTest extends DonutsDomainCheckFlowTestCase {

  @Before
  public void setUp() throws Exception {
    // Loads the epp input containing [example1.tld, example2.tld, example3.tld]
    setEppInput("domain_check_example_tlds.xml");
    createTld("tld");
    createTld("dpml.zone");
  }

  @Test
  @Ignore("This test checks the internal DPML blocks. " +
    "This logic will need to be added once all TLD's are ran from nomulus")
  public void testSuccess_internal_dpmlBlock() throws Exception {
    persistActiveDomain("example1.dpml.zone");
    persistActiveDomain("example3.dpml.zone");

    final CheckData result = runCheckFlow();

    // Verify the 2 labels registered in the dpml tld are unavailable and have
    // a reason string of 'DPML block'
    assertThat(result.getChecks())
      .containsExactlyElementsIn(
        ImmutableList.of(
          unavailableCheck("example1.tld", "DPML block"),
          availableCheck("example2.tld"),
          unavailableCheck("example3.tld", "DPML block")));
  }

  @Test
  public void testSuccess_external_dpmlBlock() throws Exception {
    persistExternalDPMLBlock("example1");

    final CheckData result = runCheckFlow();

    // Verify the label is unavailable if it exists in the BlockedLabel entity
    assertThat(result.getChecks())
      .containsExactlyElementsIn(
        ImmutableList.of(
          unavailableCheck("example1.tld", "DPML block"),
          availableCheck("example2.tld"),
          availableCheck("example3.tld")));
  }

  @Test
  public void testSuccess_external_dpmlBlock_fee() throws Exception {
    persistExternalDPMLBlock("example1");

    final String result = ofy().transact(new Work<String>() {
      @Override
      public String run() {
        try {
          return serialize(runDonutsFlow());
        } catch (Exception e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    });

    assertThat(result).contains(
      String.format(
        "<fee:fee description=\"DPML Override\">%s</fee:fee>",
        provideDpmlCreateOverridePrice().getAmount()));
  }

  @Test
  public void testReserved_IsNotBlockedByDpml() throws Exception {
    persistExternalDPMLBlock("example1");

    final ReservedList reservedList =
        persistReservedList("tld", "example1,FULLY_BLOCKED");
    final Registry tld = Registry.get("tld").asBuilder().setReservedLists(reservedList).build();
    persistResource(tld);

    final CheckData result = runCheckFlow();

    // Verify the label 'example1.tld' is unavailable due to being 'Reserved'
    assertThat(result.getChecks())
        .containsExactlyElementsIn(
            ImmutableList.of(
                unavailableCheck("example1.tld", "Reserved"),
                availableCheck("example2.tld"),
                availableCheck("example3.tld")));
  }

  @Test
  public void testPremium_IsNotBlockedByDpml() throws Exception {
    setEppInput("domain_check_premium_tlds.xml",
        ImmutableMap.of(
            "FEE_VERSION",
            "0.12"));

    // 'rich.tld' is created by default in `create(tld)` and can be found in
    // DatastoreHelper.java specifically this file `default_premium_list_testdata.csv`
    // and is defined as such [rich,USD 100]
    persistExternalDPMLBlock("rich");
    final EppResponse response = runDonutsCheckFlow();

    assertThat(response.getResult().getCode().isSuccess()).isTrue();

    final FeeCheckResponseExtension extension =
        (FeeCheckResponseExtension) response.getExtensions().get(0);

    final FeeCheckResponseExtensionItem extensionItem =
        (FeeCheckResponseExtensionItem) extension.getItems().get(0);

    assertThat(extensionItem.getFees().get(0).getCost()).isEqualTo(new BigDecimal("100.00"));
    assertThat(extensionItem.getFeeClass().toLowerCase()).isEqualTo("premium");
  }
}
