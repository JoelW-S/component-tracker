package com.joelws.componenttracker;
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

import com.joelws.componenttracker.model.Component;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.File;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.eclipse.aether.deployment.DeploymentException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentTrackerPublisher extends Notifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentTrackerPublisher.class);

  private String component;

  @DataBoundConstructor
  public ComponentTrackerPublisher(String component) {
    this.component = component;
  }

  public String getComponent() {
    return component;
  }

  public void setComponent(String component) {
    this.component = component;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {

    final EnvVars envVars = build.getEnvironment(listener);

    final String component = getComponent();

    final String componentVersion = envVars.get(component);

    String manifestFileLocation = File.separator + "tmp";

    if (!launcher.isUnix()) {
      manifestFileLocation = System.getenv("TEMP");
    }

    manifestFileLocation += File.separator + "hts-component-manifest.json";

    if (componentVersion != null) {
      try {

        MavenTransport.getInstance().archiveManifest(manifestFileLocation,
            getDescriptor().getNexusUrl(),
            getDescriptor().getNexusUser(),
            getDescriptor().getNexusPassword());

        ComponentTrackerEndpoint
            .getInstance()
            .updateComponentInManifest(component, componentVersion);


      } catch (final NumberFormatException nfe) {

        LOGGER.error("Version is wrong format", nfe);

        build.setResult(Result.FAILURE);

      } catch (DeploymentException | IOException e) {
        LOGGER.error("Deployment failed", e);
        build.setResult(Result.FAILURE);
      }

    }

    return true;
  }


  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  public ComponentTrackerPublisher.DescriptorImpl getDescriptor() {
    return Jenkins
        .getInstance()
        .getDescriptorByType(
            ComponentTrackerPublisher.DescriptorImpl.class);
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private String nexusUrl;

    private String nexusUser;

    private Secret nexusPassword;

    private List<Component> componentList;

    public DescriptorImpl() {
      load();
    }

    public String getNexusUrl() {
      return nexusUrl;
    }

    public void setNexusUrl(String nexusUrl) {
      this.nexusUrl = nexusUrl;
    }

    public String getNexusUser() {
      return nexusUser;
    }

    public void setNexusUser(String nexusUser) {
      this.nexusUser = nexusUser;
    }

    public Secret getNexusPassword() {
      return nexusPassword;
    }

    public void setNexusPassword(Secret nexusPassword) {
      this.nexusPassword = nexusPassword;
    }

    public List<Component> getComponentList() {
      return componentList;
    }

    public void setComponentList(List<Component> componentList) {
      this.componentList = componentList;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Publish manifest to nexus";
    }

    public ListBoxModel doFillComponentItems() {
      ListBoxModel items = new ListBoxModel();
      for (Component component : getComponentList()) {
        items.add(component.getName());
      }
      return items;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
        throws Descriptor.FormException {

      nexusUrl = formData.getString("nexusUrl");
      nexusUser = formData.getString("nexusUser");
      nexusPassword = Secret.fromString(formData.getString("nexusPassword"));
      componentList = req.bindJSONToList(Component.class, formData.get("componentList"));
      save();
      return super.configure(req, formData);
    }
  }


}
