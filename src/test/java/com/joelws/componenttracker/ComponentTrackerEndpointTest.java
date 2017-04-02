package com.joelws.componenttracker;

import static org.junit.Assert.assertEquals;

import com.joelws.componenttracker.model.Component;
import com.joelws.componenttracker.model.ComponentManifest;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

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
*/public class ComponentTrackerEndpointTest {

  @Rule
  public JenkinsRule j = new JenkinsRule();

  private ComponentTrackerEndpoint componentTrackerEndpoint;

  @Before
  public void setUp() throws Exception {
    componentTrackerEndpoint = new ComponentTrackerEndpoint();

    List<Component> componentList = Arrays.asList(
        new Component(
            "artifact one",
            "1.0"
        ),
        new Component(
            "artifact two",
            "1.0"
        ));

    ComponentManifest componentManifest = new ComponentManifest(
        "mock-manifest",
        "1.0.0",
        componentList
    );
    componentTrackerEndpoint.setLatestManifest(componentManifest);
  }

  @Test
  public void updateComponentInManifest() throws Exception {

    componentTrackerEndpoint.updateComponentInManifest(
        "artifact one",
        "2.0"
    );

    ComponentManifest expected = new ComponentManifest(
        "mock-manifest",
        "1.0.1",
        Arrays.asList(
            new Component(
                "artifact one",
                "2.0"
            ),
            new Component(
                "artifact two",
                "1.0"
            ))
    );

    assertEquals(expected, componentTrackerEndpoint.getLatestManifest());

  }

  @Test
  public void updateComponentInManifestNoChange() throws Exception {
    //invalid manifest version
    componentTrackerEndpoint.getLatestManifest().setVersion("1.0");

    // Calling updateComponentInManifest should do nothing as invalid manifest version

    componentTrackerEndpoint.updateComponentInManifest(
        "artifact one",
        "2.0"
    );

    ComponentManifest expected = new ComponentManifest(
        "mock-manifest",
        "1.0",
        Arrays.asList(
            new Component(
                "artifact one",
                "1.0"
            ),
            new Component(
                "artifact two",
                "1.0"
            ))
    );

    assertEquals(expected, componentTrackerEndpoint.getLatestManifest());

  }

}
