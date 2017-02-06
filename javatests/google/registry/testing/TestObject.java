// Copyright 2017 The Nomulus Authors. All Rights Reserved.
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

package google.registry.testing;

import static google.registry.model.common.EntityGroupRoot.getCrossTldKey;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import google.registry.model.ImmutableObject;
import google.registry.model.common.EntityGroupRoot;

/**
 * A test model object that can be persisted in any entity group.
 */
@Entity
public class TestObject extends ImmutableObject {
  @Parent
  Key<EntityGroupRoot> parent;

  @Id
  String id;

  String field;

  public String getId() {
    return id;
  }

  public String getField() {
    return field;
  }

  public static TestObject create(String id) {
    return create(id, null);
  }

  public static TestObject create(String id, String field) {
    return create(id, field, getCrossTldKey());
  }

  public static TestObject create(String id, String field, Key<EntityGroupRoot> parent) {
    TestObject instance = new TestObject();
    instance.id = id;
    instance.field = field;
    instance.parent = parent;
    return instance;
  }
}
