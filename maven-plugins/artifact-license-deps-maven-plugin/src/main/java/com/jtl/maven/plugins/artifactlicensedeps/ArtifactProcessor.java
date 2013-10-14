package com.jtl.maven.plugins.artifactlicensedeps;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public abstract class ArtifactProcessor {

  protected final Artifact artifact;
  protected final File buildDirectory;
  protected final ArtifactHandler artifactHandler;
  protected final MavenLogger mavenLogger;
  protected final MavenSession mavenSession;

  public ArtifactProcessor(Artifact artifact, File buildDirectory, ArtifactHandler artifactHandler, MavenLogger mavenLogger, MavenSession mavenSession) {
    this.artifact = artifact;
    this.buildDirectory = buildDirectory;
    this.artifactHandler = artifactHandler;
    this.mavenLogger = mavenLogger;
    this.mavenSession = mavenSession;
  }

  public abstract Model process() throws IOException;

  protected File writePomXmlFromDotPom() throws IOException {
    File repoPomFile = createPomFile();
    File pomFile = new File(buildDirectory, ArtifactUtils.getPomXmlFileName(artifact));
    final InputStream inputStream = new FileInputStream(repoPomFile);
    InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
      @Override
      public InputStream getInput() throws IOException {
        return inputStream;
      }
    };
    Files.copy(inputSupplier, pomFile);
    return pomFile;
  }

  protected File createPomFile() {
    if (artifact.getFile().getAbsolutePath().endsWith(".jar")) {
      return new File(artifact.getFile().getAbsolutePath().replace(".jar", ".pom"));
    } else if (artifact.getFile().getAbsolutePath().endsWith(".pom")) {
      return new File(artifact.getFile().getAbsolutePath());
    } else {
      // assume the .pom at this point
      return new File(artifact.getFile().getAbsolutePath() + ".pom");
    }
  }

  protected Model loadModel(File pomFile) throws IOException {
    DefaultModelReader modelReader = new DefaultModelReader();
    return modelReader.read(pomFile, Maps.<String, Object>newHashMap());
  }

  protected void writeLicenseNameFile(Model model) throws IOException {
    for (License license : model.getLicenses()) {
      String licenseName = license.getName().replace(" ", "_");
      File licenseFile = new File(buildDirectory, ArtifactUtils.getPomLicenseFileName(artifact, licenseName));
      Files.touch(licenseFile);
    }
  }

  protected Set<Artifact> toArtifacts(final List<Dependency> dependencies) {
    return Sets.newHashSet(Iterables.transform(dependencies, new Function<Dependency, Artifact>() {
      @Override
      public Artifact apply(Dependency dependency) {
        Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                dependency.getScope(), dependency.getType(), dependency.getClassifier(), artifactHandler);
        artifact.setOptional(dependency.isOptional());
        return artifact;
      }
    }));
  }

}
