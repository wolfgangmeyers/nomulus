package domains.donuts.flows;

import domains.donuts.flows.DonutsEppTestComponent.FakesAndMocksModule;
import google.registry.flows.Flow;
import google.registry.flows.ResourceFlowTestCase;
import google.registry.flows.picker.FlowPicker;
import google.registry.model.EppResource;
import google.registry.model.eppoutput.EppOutput;
import google.registry.util.TypeUtils;

import static com.google.common.truth.Truth.assertThat;

public abstract class DonutsResourceFlowTestCase<
        F extends Flow, R extends EppResource>
    extends ResourceFlowTestCase<F, R> {

  /** Method to allow us to load DonutsEppTestComponent in place of the standard EppTestComponent */
  @SuppressWarnings("unchecked")
  protected EppOutput runDonutsFlow() throws Exception {
    assertThat(FlowPicker.getFlowClass(eppLoader.getEpp()))
        .isEqualTo(new TypeUtils.TypeInstantiator<F>(getClass()) {}.getExactType());
    return
        DaggerDonutsEppTestComponent.builder()
            .fakesAndMocksModule(new FakesAndMocksModule(clock))
            .build()
            .startRequest()
            .flowComponentBuilder()
            .flowModule(createFlowModule(CommitMode.LIVE, UserPrivileges.NORMAL))
            .build()
            .flowRunner()
            .run();
//            .getResponse()
//            .getResponseData()
//            .get(0);
  }
}
