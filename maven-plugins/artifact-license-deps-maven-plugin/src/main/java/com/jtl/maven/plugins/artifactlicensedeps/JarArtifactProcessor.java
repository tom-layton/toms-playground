package com.jtl.maven.plugins.artifactlicensedeps;

import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarArtifactProcessor extends ArtifactProcessor {

  public JarArtifactProcessor(Artifact artifact, File buildDirectory, ArtifactHandler artifactHandler, MavenLogger mavenLogger, MavenSession mavenSession) {
    super(artifact, buildDirectory, artifactHandler, mavenLogger, mavenSession);
  }

  @Override
  public Model process() throws IOException {
    JarFile artifactJarFile = null;
    try {
      artifactJarFile = createJarFile();
      if (artifactJarFile == null) {
        mavenLogger.logWarn(artifact, "Skipping JAR processing. Returning.");
        return null;
      }

      File pomFile = writePomFile(artifactJarFile);
      Model model = loadModel(pomFile);

      writeLicenseNameFile(model);
      writeLicenseFile(artifactJarFile);
      writeDependenciesFile(artifactJarFile);
      return model;
    } finally {
      if (artifactJarFile != null) {
        artifactJarFile.close();
      }
    }
  }

  private JarFile createJarFile() throws IOException {
    try {
      String extension = FileUtils.extension(artifact.getFile().getAbsolutePath());
      if (!artifact.getFile().isDirectory() && !"jar".equals(extension)) {
        return new JarFile(new File(artifact.getFile().getAbsolutePath() + ".jar"));
      } else {
        return new JarFile(artifact.getFile());
      }
    } catch (FileNotFoundException e) {
      mavenLogger.logWarn(artifact, "Could not find JAR in local repo", e);
      return null;
    }
  }

  private File writePomFile(JarFile jarFile) throws IOException {
    JarEntry pomEntry = findPomEntry(jarFile);
    if (pomEntry != null) {
      return writePomXmlFromJar(jarFile, pomEntry);
    } else {
      return writePomXmlFromDotPom();
    }
  }

  private File writePomXmlFromJar(JarFile jarFile, JarEntry jarEntry) throws IOException {
    File pomFile = new File(buildDirectory, ArtifactUtils.getPomXmlFileName(artifact));
    final InputStream inputStream = jarFile.getInputStream(jarEntry);
    InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
      @Override
      public InputStream getInput() throws IOException {
        return inputStream;
      }
    };
    Files.copy(inputSupplier, pomFile);
    return pomFile;
  }

  private JarEntry findPomEntry(final JarFile jarFile) {
    return jarFile.getJarEntry("META-INF/maven/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/pom.xml");
  }

  private JarEntry findPomPropertiesEntry(final JarFile jarFile) {
    return jarFile.getJarEntry("META-INF/maven/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/pom.properties");
  }

  private void writeLicenseFile(final JarFile jarFile) throws IOException {
    JarEntry licenseEntry = findLicenseEntry(jarFile);
    if (licenseEntry != null) {
      File licenseFile = new File(buildDirectory, ArtifactUtils.getLicenseFileName(artifact, licenseEntry.getName()));
      final InputStream inputStream = jarFile.getInputStream(licenseEntry);
      InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
        @Override
        public InputStream getInput() throws IOException {
          return inputStream;
        }
      };
      Files.copy(inputSupplier, licenseFile);
    }
  }

  private JarEntry findLicenseEntry(final JarFile jarFile) {
    final Set<String> licenseEntryNames = Sets.newHashSet("META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/license");
    for (String licenseEntryName : licenseEntryNames) {
      JarEntry jarEntry = jarFile.getJarEntry(licenseEntryName);
      if (jarEntry != null) {
        return jarEntry;
      }
    }
    return null;
  }

  private void writeDependenciesFile(final JarFile jarFile) throws IOException {
    JarEntry dependenciesEntry = findDependenciesEntry(jarFile);
    if (dependenciesEntry != null) {
      File licenseFile = new File(buildDirectory, ArtifactUtils.getDependenciesFileName(artifact, dependenciesEntry.getName()));
      final InputStream inputStream = jarFile.getInputStream(dependenciesEntry);
      InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
        @Override
        public InputStream getInput() throws IOException {
          return inputStream;
        }
      };
      Files.copy(inputSupplier, licenseFile);
    }
  }

  private JarEntry findDependenciesEntry(final JarFile jarFile) {
    final Set<String> dependencyEntryNames = Sets.newHashSet("META-INF/DEPENDENCIES", "META-INF/dependencies");
    for (String licenseEntryName : dependencyEntryNames) {
      JarEntry jarEntry = jarFile.getJarEntry(licenseEntryName);
      if (jarEntry != null) {
        return jarEntry;
      }
    }
    return null;
  }

}
