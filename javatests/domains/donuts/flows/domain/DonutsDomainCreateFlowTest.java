package domains.donuts.flows.domain;

import com.google.appengine.labs.repackaged.com.google.common.collect.ImmutableMap;
import domains.donuts.flows.custom.DonutsDomainCreateFlowCustomLogic.DpmlBlockedException;
import domains.donuts.flows.custom.DonutsDomainCreateFlowCustomLogic.SignedMarksRequiredException;
import google.registry.model.eppoutput.Result;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static domains.donuts.config.DonutsConfigModule.provideDpmlTld;
import static google.registry.model.eppoutput.Result.Code.SUCCESS;
import static google.registry.testing.DatastoreHelper.createTld;
import static google.registry.testing.DatastoreHelper.persistActiveDomain;

public class DonutsDomainCreateFlowTest extends DonutsDomainCreateFlowTestCase {

  private final String dpml_tld = provideDpmlTld();

  @Before
  public void setUp() throws Exception {
    setEppInput("domain_create.xml");
    createTld("tld");
    createTld(dpml_tld);
    persistNetContactsAndHosts();
  }

  @Test
  public void testCreateBlockedByDpml() throws Exception {
    setEppInput("domain_create.xml");
    persistActiveDomain("example." + dpml_tld);
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
  public void testDpmlOverride() throws Exception {
    setEppInput("domain_create_dpml_phase_encoded_smd.xml", ImmutableMap.of("FEE_VERSION", "0.12"));
    persistActiveDomain("test-validate." + dpml_tld);
    final Result result = runDonutsFlow().getResponse().getResult();
    assertThat(result).isEqualTo(Result.create(SUCCESS));
  }
}
