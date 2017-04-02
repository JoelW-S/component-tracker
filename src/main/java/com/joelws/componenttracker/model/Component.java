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

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class Component extends AbstractDescribableImpl<Component> {

  private String name;

  private String version;

  @DataBoundConstructor
  public Component(String name, String version) {
    this.name = name;
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Component component = (Component) o;

    if (name != null ? !name.equals(component.name) : component.name != null) {
      return false;
    }
    return version != null ? version.equals(component.version) : component.version == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Component> {

    public FormValidation doCheckName(@QueryParameter String value) {
      if (value.isEmpty()) {
        return FormValidation.error("Can't be empty!");
      }
      return FormValidation.ok();
    }

    @Override
    public String getDisplayName() {
      return "";
    }
  }
}
