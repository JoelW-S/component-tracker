package com.joelws.componenttracker.model;
/*
Copyright 2016 Joel Whittaker-Smith

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class ComponentManifest extends Component {

  private List<Component> artifacts;

  @DataBoundConstructor
  public ComponentManifest(String name, String version, List<Component> artifacts) {
    super(name, version);
    this.artifacts = artifacts;
  }

  public List<Component> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(List<Component> artifacts) {
    this.artifacts = artifacts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    ComponentManifest that = (ComponentManifest) o;

    return artifacts != null ? artifacts.equals(that.artifacts) : that.artifacts == null;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (artifacts != null ? artifacts.hashCode() : 0);
    return result;
  }
}
