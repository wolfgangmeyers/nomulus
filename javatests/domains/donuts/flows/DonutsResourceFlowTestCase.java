package domains.donuts.flows;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.flows.EppXmlTransformer.marshal;
import static java.nio.charset.StandardCharsets.UTF_8;

import domains.donuts.flows.DonutsEppTestComponent.FakesAndMocksModule;
import google.registry.flows.Flow;
import google.registry.flows.ResourceFlowTestCase;
import google.registry.flows.picker.FlowPicker;
import google.registry.model.EppResource;
import google.registry.model.eppoutput.EppOutput;
import google.registry.util.TypeUtils;
import google.registry.xml.ValidationMode;
import google.registry.xml.XmlException;

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
  }

  /** Method used to access Google's nested private EPP exceptions **/
  @SuppressWarnings("unchecked")
  protected Class<? extends Throwable> getNestedPrivateThrowableClass(
      final String className, final Class containingClass) {
    for (Class clazz : containingClass.getDeclaredClasses()) {
      if (className.equalsIgnoreCase(clazz.getSimpleName())) {
        return clazz;
      }
    }
    throw new IllegalStateException(
        String.format("Class [%s] does not exist in class [%s]", className, containingClass.getSimpleName()));
  }

  protected String serialize(final EppOutput eppOutput) throws XmlException {
    return new String(marshal(eppOutput, ValidationMode.LENIENT), UTF_8);
  }
}
