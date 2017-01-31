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

package domains.donuts.flows;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.flows.EppXmlTransformer.marshal;
import static google.registry.testing.DatastoreHelper.persistResource;
import static java.nio.charset.StandardCharsets.UTF_8;

import domains.donuts.flows.DonutsEppTestComponent.FakesAndMocksModule;
import google.registry.flows.Flow;
import google.registry.flows.ResourceFlowTestCase;
import google.registry.flows.picker.FlowPicker;
import google.registry.model.EppResource;
import google.registry.model.eppoutput.EppOutput;
import google.registry.model.external.BlockedLabel;
import google.registry.model.external.BlockedLabel.Builder;
import google.registry.util.TypeUtils;
import google.registry.xml.ValidationMode;
import google.registry.xml.XmlException;
import java.lang.reflect.Field;
import org.joda.time.DateTime;

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

  /** Method used to return field data using reflection **/
  @SuppressWarnings("unchecked")
  protected static <T> T getField(final Class clazz, final String fieldName, final Object instance)
      throws Exception {
    try {
      final Field declaredField = clazz.getDeclaredField(fieldName);
      declaredField.setAccessible(true);
      return (T) declaredField.get(instance);
    } catch (NoSuchFieldException e) {
      Class superClass = clazz.getSuperclass();
      if (superClass == null) {
        throw e;
      } else {
        return getField(superClass, fieldName, instance);
      }
    }
  }

  protected String serialize(final EppOutput eppOutput) throws XmlException {
    return new String(marshal(eppOutput, ValidationMode.LENIENT), UTF_8);
  }

  protected BlockedLabel persistExternalDPMLBlock(final String label) throws Exception {
    return persistResource(
        new Builder()
            .setLabel(label)
            .setDateCreated(DateTime.now())
            .setDateModified(DateTime.now())
            .build());
  }
}
