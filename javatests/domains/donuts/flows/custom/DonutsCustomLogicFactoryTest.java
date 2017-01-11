package domains.donuts.flows.custom;

import static com.google.common.truth.Truth.assertThat;

import google.registry.flows.SessionMetadata;
import google.registry.model.eppinput.EppInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DonutsCustomLogicFactoryTest {

  @Mock private EppInput eppInput;
  @Mock private SessionMetadata sessionMetadata;

  private final DonutsCustomLogicFactory tested = new DonutsCustomLogicFactory();

  @Test
  public void testForDomainCreateFlow() throws Exception {
    assertThat(tested.forDomainCreateFlow(eppInput, sessionMetadata))
        .isInstanceOf(DonutsDomainCreateFlowCustomLogic.class);
  }

  @Test
  public void testForDomainCheckFlow() throws Exception {
    assertThat(tested.forDomainCheckFlow(eppInput, sessionMetadata))
        .isInstanceOf(DonutsDomainCheckFlowCustomLogic.class);
  }

  @Test
  public void testForDomainPricing() throws Exception {
    assertThat(tested.forDomainPricing(eppInput, sessionMetadata))
        .isInstanceOf(DonutsDomainPricingCustomLogic.class);
  }
}
