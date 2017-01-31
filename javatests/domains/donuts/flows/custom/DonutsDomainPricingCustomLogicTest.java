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

package domains.donuts.flows.custom;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistResource;
import static google.registry.util.DateTimeUtils.END_OF_TIME;
import static google.registry.util.DateTimeUtils.START_OF_TIME;
import static org.joda.money.CurrencyUnit.USD;
import static org.mockito.Mockito.doReturn;

import com.google.appengine.labs.repackaged.com.google.common.collect.Range;
import com.google.common.collect.ImmutableList;
import com.google.common.net.InternetDomainName;
import google.registry.flows.FlowMetadata;
import google.registry.flows.SessionMetadata;
import google.registry.flows.custom.DomainPricingCustomLogic.CreatePriceParameters;
import google.registry.flows.domain.FeesAndCredits;
import google.registry.model.domain.fee.BaseFee.FeeType;
import google.registry.model.domain.fee.Fee;
import google.registry.model.eppinput.EppInput;
import google.registry.model.external.BlockedLabel;
import google.registry.testing.AppEngineRule;
import org.joda.money.Money;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

@RunWith(MockitoJUnitRunner.class)
public class DonutsDomainPricingCustomLogicTest {

  @Rule public final AppEngineRule appEngine = AppEngineRule.builder().withDatastore().build();
  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Mock private EppInput eppInput;
  @Mock private SessionMetadata sessionMetadata;
  @Mock private CreatePriceParameters priceParameters;
  @Mock private FlowMetadata flowMetadata;

  private FeesAndCredits createFees;
  private DonutsDomainPricingCustomLogic tested;

  @Before
  public void setUp() throws Exception {
    createTld("tld");
    doReturn(InternetDomainName.from("sld.tld")).when(priceParameters).domainName();
    doReturn(DateTime.now()).when(priceParameters).asOfDate();
    createFees = new FeesAndCredits.Builder()
      .addFeeOrCredit(
        Fee.create(BigDecimal.ZERO, FeeType.CREATE, Range.closedOpen(START_OF_TIME, END_OF_TIME)))
      .setCurrency(USD)
      .build();
    doReturn(createFees).when(priceParameters).feesAndCredits();
    tested = new DonutsDomainPricingCustomLogic(eppInput, sessionMetadata, flowMetadata);
  }

  @Test
  public void testCustomizeCreatePrice_Premium() throws Exception {
    doReturn(InternetDomainName.from("rich.tld")).when(priceParameters).domainName();
    final FeesAndCredits result = tested.customizeCreatePrice(priceParameters);
    // The default create should be returned
    assertThat(result).isEqualTo(createFees);
  }

  @Test
  public void testCustomizeCreatePrice_NotDpml() throws Exception {
    final FeesAndCredits result = tested.customizeCreatePrice(priceParameters);
    // The default create should be returned
    assertThat(result).isEqualTo(createFees);
  }

  @Test
  public void testCustomizeCreatePrice() throws Exception {
    // Note: This test will need to be updated once the DPML lookup has been switched to use internal
    persistResource(new BlockedLabel.Builder()
      .setLabel("sld")
      .setDateCreated(DateTime.now())
      .setDateModified(DateTime.now())
      .build());

    final ImmutableList<Fee> result = tested.customizeCreatePrice(priceParameters).getFees();
    assertThat(result).hasSize(2);
    assertThat(result).contains(Fee.create(Money.of(USD, 25.00).getAmount(), FeeType.DPML));
  }
}
