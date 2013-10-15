package com.jtl.maven.plugins.artifactlicensedeps;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;

import java.io.File;
import java.io.IOException;

public class PomArtifactProcessor extends ArtifactProcessor {

  public PomArtifactProcessor(Artifact artifact, File buildDirectory, ArtifactHandler artifactHandler, MavenLogger mavenLogger, MavenSession mavenSession) {
    super(artifact, buildDirectory, artifactHandler, mavenLogger, mavenSession);
  }

  @Override
  public Model process() throws IOException {
    File pomFile = writePomXmlFromDotPom();
    Model model = loadModel(pomFile);

    writeLicenseNameFile(model);
//    writePomDependenciesToFile(model);

    return model;
  }

//  private void writePomDependenciesToFile(Model model) {
    // TODO: pass pomDependenciesFile as an argument
//    File pomDependenciesFile = new File(buildDirectory + File.separator + artifact.getArtifactId() + "-DEPENDENCIES-POM.txt");
//    for (Dependency dependency : model.getDependencies()) {
//    }
//  }
}
