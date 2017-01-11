package domains.donuts.flows.domain;

import static com.google.common.truth.Truth.assertThat;
import static domains.donuts.config.DonutsConfigModule.provideDpmlCreateOverridePrice;
import static google.registry.model.ofy.ObjectifyService.ofy;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistActiveDomain;
import static google.registry.testing.DatastoreHelper.persistResource;

import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Work;
import google.registry.model.eppoutput.CheckData;
import google.registry.model.external.BlockedLabel;
import org.joda.time.DateTime;
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

    final CheckData result = runDonutsCheckFlow();

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
    persistResource(
      new BlockedLabel.Builder()
        .setLabel("example1")
        .setDateCreated(DateTime.now())
        .setDateModified(DateTime.now())
        .build());

    final CheckData result = ofy().transact(new Work<CheckData>() {
      @Override
      public CheckData run() {
        try {
          return runDonutsCheckFlow();
        } catch (Exception e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    });

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
    persistResource(
      new BlockedLabel.Builder()
        .setLabel("example1")
        .setDateCreated(DateTime.now())
        .setDateModified(DateTime.now())
        .build());

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
}
