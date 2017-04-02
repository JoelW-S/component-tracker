package com.joelws.componenttracker;/*
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
import com.joelws.componenttracker.model.ComponentManifest;
import hudson.util.Secret;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenTransport {

  private static final MavenTransport INSTANCE = new MavenTransport();

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenTransport.class);

  private MavenTransport() {
  }

  public static MavenTransport getInstance() {
    return INSTANCE;
  }


  public synchronized void archiveManifest(final String manifestFileLocation,
      final String nexusUrl,
      final String nexusUser,
      final Secret nexusPassword)
      throws DeploymentException, IOException {

    final ComponentManifest componentManifest = ComponentTrackerEndpoint.getInstance()
        .getLatestManifest();
    LOGGER.debug("Archiving component Manifest: " + componentManifest.getName());

    RepositorySystem system = newRepositorySystem();
    RepositorySystemSession session = newSession(system);

    Gson gson = new Gson();

    Artifact artifact = new DefaultArtifact(
        "net.atos.hts",
        componentManifest.getName(),
        "",
        "json",
        componentManifest.getVersion());

    final String manifestAsJson = gson.toJson(componentManifest);

    LOGGER.debug("Writing manifest file temporarily to filesystem...");

    LOGGER.debug(manifestAsJson);

    BufferedWriter writer = new BufferedWriter(new FileWriter(manifestFileLocation));
    writer.write(manifestAsJson);
    writer.close();

    final File temporaryManifestFile = new File(manifestFileLocation);

    artifact = artifact.setFile(temporaryManifestFile);

    Authentication authentication = new AuthenticationBuilder()
        .addUsername(nexusUser)
        .addPassword(Secret.toString(nexusPassword))
        .build();

    RemoteRepository releaseRepo = new RemoteRepository.Builder(
        "releases",
        "default",
        nexusUrl + "/" + "content/repositories/releases")
        .setAuthentication(authentication)
        .build();

    DeployRequest deployRequest = new DeployRequest();
    deployRequest.addArtifact(artifact);
    deployRequest.setRepository(releaseRepo);

    LOGGER.debug("Deploying to nexus: " + nexusUrl);
    system.deploy(session, deployRequest);
  }


  private RepositorySystem newRepositorySystem() {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
    return locator.getService(RepositorySystem.class);
  }

  private RepositorySystemSession newSession(RepositorySystem system) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepo = new LocalRepository(
        new File(System.getProperty("user.home"),
            ".m2/repository"));
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
    return session;
  }
}
