package domains.donuts.flows.domain;

import com.google.appengine.labs.repackaged.com.google.common.collect.ImmutableList;
import google.registry.model.eppoutput.CheckData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistActiveDomain;

@RunWith(MockitoJUnitRunner.class)
public class DonutsDomainCheckFlowTest extends DonutsDomainCheckFlowTestCase {

  @Before
  public void setUp() throws Exception {
    // Loads the epp input containing [example1.tld, example2.tld, example3.tld]
    setEppInput("domain_check_example_tlds.xml");
    createTld("tld");
  }

  @Test
  public void testSuccess_dpmlBlock() throws Exception {
    createTld("dpml.zone");
    persistActiveDomain("example1.dpml.zone");
    persistActiveDomain("example3.dpml.zone");

    final CheckData result = runDonutsCheckFlow();

    // Verify the 2 labels registered in the dpml tld are unavailable and have
    // a reason string of 'DPML block'
    assertThat(result.getChecks()).containsExactlyElementsIn(
        ImmutableList.of(
          unavailableCheck("example1.tld", "DPML block"),
          availableCheck("example2.tld"),
          unavailableCheck("example3.tld", "DPML block")));
  }
}
