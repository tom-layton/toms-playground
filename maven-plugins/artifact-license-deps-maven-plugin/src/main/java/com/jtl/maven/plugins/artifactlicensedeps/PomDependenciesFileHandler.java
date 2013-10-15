package com.jtl.maven.plugins.artifactlicensedeps;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

public class PomDependenciesFileHandler {
  // groupId, artifactId, version, type
  private static final String ARTIFACT_HEADER_FORMAT = "%s:%s:%s:%s - POM Derived Artifact Dependencies";
  // groupId, artifactId, version, type
  private static final String DEPENDENCY_MESSAGE_FORMAT = "\t%s:%s:%s:%s";

  private final Artifact artifact;
  private final File buildDirectory;
  private final String dependencyType;
  private final MavenLogger mavenLogger;

  private BufferedWriter pomDependenciesFileWriter;

  public PomDependenciesFileHandler(Artifact artifact, File buildDirectory, String dependencyType, MavenLogger mavenLogger) throws FileNotFoundException {
    this.artifact = artifact;
    this.buildDirectory = buildDirectory;
    this.dependencyType = dependencyType;
    this.mavenLogger = mavenLogger;
    initialize();
  }

  private void initialize() throws FileNotFoundException {
    mavenLogger.logInfo(artifact, "Creating file for dependencies found in the POM");
    String pomDependencyFileName = Files.simplifyPath(buildDirectory.getAbsolutePath()) + File.separator + ArtifactUtils.getPomDependenciesFileName(artifact);
    File pomDependenciesFile = new File(pomDependencyFileName);
    FileUtils.deleteQuietly(pomDependenciesFile);
    mavenLogger.logInfo(artifact, "File is " + pomDependenciesFile.getAbsolutePath());

    pomDependenciesFileWriter = Files.newWriter(pomDependenciesFile, Charset.defaultCharset());
  }

  public void writeHeader() throws IOException {
    pomDependenciesFileWriter.write(String.format(ARTIFACT_HEADER_FORMAT, artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getVersion(), dependencyType));
    pomDependenciesFileWriter.newLine();
    pomDependenciesFileWriter.newLine();
  }

  public void writeArtifactDependency(Artifact artifactDependency) throws IOException {
    pomDependenciesFileWriter.write(String.format(DEPENDENCY_MESSAGE_FORMAT, artifactDependency.getGroupId(), artifactDependency.getArtifactId(),
            artifactDependency.getVersion(), artifactDependency.getType()));
    pomDependenciesFileWriter.newLine();
  }

  public void close() {
    Closeables.closeQuietly(pomDependenciesFileWriter);
  }
}
