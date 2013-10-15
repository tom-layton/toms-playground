package com.jtl.maven.plugins.artifactlicensedeps;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.apache.maven.artifact.Artifact;

/**
 * @author e116n0
 */
public abstract class ArtifactUtils {

  public static String getPomXmlFileName(Artifact artifact) {
    return String.format("%s-POM.xml", getFileNamePrefix(artifact));
  }

  public static String getPomDependenciesFileName(Artifact artifact) {
    return String.format("%s-POM-DEPENDENCIES.txt", getFileNamePrefix(artifact));
  }

  public static String getPomLicenseFileName(Artifact artifact, String licenseName) {
    return String.format("%s-POM-LICENSE-%s.txt", getFileNamePrefix(artifact), licenseName);
  }

  public static String getDependenciesFileName(Artifact artifact, String dependenciesFileName) {
    return String.format("%s-%s", getFileNamePrefix(artifact), cleanupFileName(dependenciesFileName));
  }

  public static String getLicenseFileName(Artifact artifact, String licenseFileName) {
    return String.format("%s-%s", getFileNamePrefix(artifact), cleanupFileName(licenseFileName));
  }

  private static String getFileNamePrefix(Artifact artifact) {
    return String.format("%s..%s..%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
  }

  private static String cleanupFileName(String fileName) {
    return Iterables.getLast(Splitter.on('/').split(Files.simplifyPath(fileName)));
  }
}
