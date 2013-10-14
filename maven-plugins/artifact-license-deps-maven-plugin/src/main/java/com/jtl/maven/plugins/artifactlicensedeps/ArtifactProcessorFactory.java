package com.jtl.maven.plugins.artifactlicensedeps;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;

import java.io.File;

public class ArtifactProcessorFactory {

  public static ArtifactProcessor getArtifactProcessor(Artifact artifact, File buildDirectory, ArtifactHandler artifactHandler,
                                                       MavenLogger mavenLogger, MavenSession mavenSession) {
    if (artifact.getType() == null || "jar".equals(artifact.getType())) {
      return new JarArtifactProcessor(artifact, buildDirectory, artifactHandler, mavenLogger, mavenSession);
    } else if ("pom".equals(artifact.getType())) {
      return new PomArtifactProcessor(artifact, buildDirectory, artifactHandler, mavenLogger, mavenSession);
    }
    return null;
  }
}
