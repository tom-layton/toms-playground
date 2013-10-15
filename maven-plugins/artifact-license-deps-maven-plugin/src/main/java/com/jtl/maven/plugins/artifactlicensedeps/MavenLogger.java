package com.jtl.maven.plugins.artifactlicensedeps;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;

import java.util.Map;

public class MavenLogger {
  private static final String ARTIFACT_MESSAGE_FORMAT = "%s:%s:%s:%s - %s";
  private static final String ARTIFACT_AND_DEPENDENCY_MESSAGE_FORMAT = "%s:%s:%s:%s - Converted to %s:%s%s%s";
  private final Log log;

  public MavenLogger(final Log log) {
    this.log = log;
  }

  public Log getLog() {
    return log;
  }

  public void logInfo(final Dependency dependency, String message) {
    getLog().info(String.format(ARTIFACT_MESSAGE_FORMAT, dependency.getGroupId(), dependency.getArtifactId(),
            dependency.getVersion(), dependency.getType(), message));
  }

  public void logInfo(final Artifact artifact, String message) {
    getLog().info(String.format(ARTIFACT_MESSAGE_FORMAT, artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getVersion(), artifact.getType(), message));
  }

  public void logInfo(final Artifact artifact, Artifact dependencyArtifact, String message) {
    getLog().info(String.format(ARTIFACT_AND_DEPENDENCY_MESSAGE_FORMAT,
            artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType(),
            dependencyArtifact.getGroupId(), dependencyArtifact.getArtifactId(), dependencyArtifact.getVersion(), dependencyArtifact.getType()));
  }

  public void logWarn(final Artifact artifact, String message) {
    getLog().warn(String.format(ARTIFACT_MESSAGE_FORMAT, artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getVersion(), artifact.getType(), message));
  }

  public void logWarn(final Artifact artifact, String message, Throwable throwable) {
    getLog().warn(String.format(ARTIFACT_MESSAGE_FORMAT, artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getVersion(), artifact.getType(), message)); //, throwable); // too much output for now
  }

  public void logError(final Artifact artifact, String message) {
    getLog().error(String.format(ARTIFACT_MESSAGE_FORMAT, artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getVersion(), artifact.getType(), message));
  }

  public void logError(final Artifact artifact, String message, Throwable throwable) {
    getLog().error(String.format(ARTIFACT_MESSAGE_FORMAT, artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getVersion(), artifact.getType(), message)); //, throwable); // too much output for now
  }

  public void dumpArtifact(final Artifact artifact) {
    logInfo(artifact, "Dumping artifact");

    for (String dependencyTrail : artifact.getDependencyTrail()) {
      logInfo(artifact, String.format("--> Dependency trail - %s", dependencyTrail));
    }
    logInfo(artifact, String.format("--> Download URL - %s", artifact.getDownloadUrl()));
    logInfo(artifact, String.format("--> File -  %s", artifact.getFile()));
    logInfo(artifact, String.format("--> Repository - %s", artifact.getRepository()));

    if (artifact.getMetadataList() != null) {
      for (ArtifactMetadata artifactMetadata : artifact.getMetadataList()) {
        logInfo(artifact, String.format("--> Metadata - %s", artifactMetadata));
        if (artifact.getRepository() != null) {
//          logInfo(artifact, String.format("--> Local Repository Metadata - %s", artifact.getRepository().pathOfLocalRepositoryMetadata(artifactMetadata)));
          logInfo(artifact, String.format("--> Remote Repository Metadata - %s", artifact.getRepository().pathOfRemoteRepositoryMetadata(artifactMetadata)));
        }
      }
    }
  }

  public void dumpModelProperties(final Model model) {
    getLog().info("Dumping model properties");

    if (model.getProperties() != null) {
      for (Map.Entry<Object, Object> modelPropertyEntry : model.getProperties().entrySet()) {
        getLog().info(String.format("--> %s = %s", modelPropertyEntry.getKey(), modelPropertyEntry.getValue()));
      }
    }
  }
}
