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

import com.google.gson.Gson;
import com.joelws.componenttracker.model.Component;
import com.joelws.componenttracker.model.ComponentManifest;
import hudson.BulkChange;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.RootAction;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.security.AccessControlled;
import hudson.util.IOUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class ComponentTrackerEndpoint implements RootAction, Saveable {

  private final static Logger LOGGER = LoggerFactory.getLogger(ComponentTrackerEndpoint.class);

  private static final String ENDPOINT_URL = "component-tracker";

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

  private ComponentManifest latestManifest;

  public ComponentTrackerEndpoint() {
    load();
  }

  public static ComponentTrackerEndpoint getInstance() {
    return Jenkins
        .getInstance()
        .getExtensionList(Action.class)
        .get(ComponentTrackerEndpoint.class);
  }

  @Override
  public String getIconFileName() {
    return null;
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public String getUrlName() {
    return ENDPOINT_URL;
  }

  public synchronized ComponentManifest getLatestManifest() {
    return latestManifest;
  }

  public synchronized void setLatestManifest(ComponentManifest latestManifest) {
    this.latestManifest = latestManifest;
    save();
  }

  public synchronized void updateComponentInManifest(String name, String version)
      throws NumberFormatException {
    ComponentManifest latestManifest = getLatestManifest();
    Component componentToBeUpdated = null;

    for (Component component : latestManifest.getArtifacts()) {
      if (name.equals(component.getName())) {
        componentToBeUpdated = component;
      }
    }

    if (componentToBeUpdated != null) {
      final Matcher versionMatcher = VERSION_PATTERN.matcher(latestManifest.getVersion());

      if (versionMatcher.matches()) {
        final int incrementalNumber = Integer.parseInt(versionMatcher.group(3));
        latestManifest.setVersion(String.format(
            "%s.%s.%s",
            versionMatcher.group(1),
            versionMatcher.group(2),
            incrementalNumber + 1));
      }

      componentToBeUpdated.setVersion(version);
    }
    setLatestManifest(latestManifest);
  }

  @Override
  public synchronized void save() {
    if (!BulkChange.contains(this)) {
      try {
        LOGGER.debug("Persisting state to filesystem");
        getConfigFile().write(this);
        SaveableListener.fireOnChange(this, getConfigFile());
      } catch (IOException e) {
        LOGGER.error(e.getMessage());
      }
    }
  }

  private synchronized void load() {
    XmlFile file = getConfigFile();

    if (file.exists()) {
      try {
        LOGGER.debug("Loading state from filesystem");
        file.unmarshal(this);
      } catch (IOException e) {
        LOGGER.warn(String.format("Failed to load %s", file.getFile().getName()));
      }
    }
  }

  public void doDynamic(StaplerRequest req, StaplerResponse res) throws IOException {
    AccessControlled accessControlled = Jenkins.getInstance();
    boolean hasPermission = accessControlled.hasPermission(Jenkins.ADMINISTER);

    if (hasPermission) {

      try {

        handleHttpMethod(req, res);

      } catch (IOException e) {

        res.sendError(400);

      }
    } else {
      res.sendError(403);
    }


  }

  private XmlFile getConfigFile() {
    return new XmlFile(new File(Jenkins.getInstance().getRootDir(), getClass().getName() + ".xml"));
  }

  private void handleHttpMethod(StaplerRequest req, StaplerResponse res) throws IOException {
    final String method = req.getMethod();
    if ("GET".equals(method)) {
      handleGet(req, res);
    } else if ("POST".equals(method)) {
      handlePost(req, res);
    }
  }

  private void handleGet(StaplerRequest req, StaplerResponse res) throws IOException {

    if (req.getMethod().equals("GET")) {

      Gson gson = new Gson();
      final String latestManifest = gson.toJson(getLatestManifest());

      LOGGER.info("Getting latest manifest version");

      res
          .addHeader("Content-Type", "application/json");
      res
          .getWriter()
          .print(latestManifest);
    }
  }

  private void handlePost(StaplerRequest req, StaplerResponse res) throws IOException {
    if (req.getMethod().equals("POST")
        && "application/json"
        .equals(req.getHeader("Content-Type"))) {

      LOGGER.info("Setting latest manifest");
      final JSONObject jsonObject = JSONObject.fromObject(getRequestBody(req));

      if (jsonObject != null) {

        setLatestManifest(req.bindJSON(ComponentManifest.class, jsonObject));
        res.setStatus(200);
        return;

      }
    }
    res.sendError(400, "NOT VALID");
  }

  private String getRequestBody(StaplerRequest req) {
    String body = "";
    BufferedReader br;

    try (BufferedReader bufferedReader = br = req.getReader()) {
      body = IOUtils.toString(br);
    } catch (IOException e) {
      return body;
    }
    return body;

  }

}
