package com.jtl.maven.plugins.artifactlicensedeps;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.merge.ModelMerger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class MainArtifactProcessor {
  private final Artifact artifact;
  private final ArtifactHandler artifactHandler;
  private final File buildDirectory;
  private final MavenLogger mavenLogger;
  private final MavenSession mavenSession;

  private File dependencyBuildDirectory;
  private PomDependenciesFileHandler pomDependenciesFileHandler;
  private Set<Artifact> processedArtifacts = Sets.newHashSet();

  public MainArtifactProcessor(Artifact artifact, ArtifactHandler artifactHandler, File buildDirectory, MavenLogger mavenLogger, MavenSession mavenSession) {
    this.artifact = artifact;
    this.artifactHandler = artifactHandler;
    this.buildDirectory = buildDirectory;
    this.mavenLogger = mavenLogger;
    this.mavenSession = mavenSession;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void process() throws IOException {
    mavenLogger.logInfo(artifact, "START Main Artifact");

    try {
      String dependencyType = Objects.firstNonNull(artifact.getType(), "jar");
      String dependencyBuildDirName = Files.simplifyPath(buildDirectory.getAbsolutePath()) + File.separator + artifact.getArtifactId() + "-" + artifact.getVersion() + "-" + dependencyType;

      mavenLogger.logInfo(artifact, "Creating build directory");
      dependencyBuildDirectory = new File(dependencyBuildDirName);
      dependencyBuildDirectory.mkdirs();
      mavenLogger.logInfo(artifact, "Build directory is " + dependencyBuildDirectory.getAbsolutePath());

      pomDependenciesFileHandler = new PomDependenciesFileHandler(artifact, dependencyBuildDirectory, dependencyType, mavenLogger);
      pomDependenciesFileHandler.writeHeader();

      processMainArtifact();

      pomDependenciesFileHandler.close();

      // zip dependencyBuildDirFile
      File zipFile = new File(dependencyBuildDirName + ".zip");
      new DirectoryZipper(zipFile, dependencyBuildDirectory).zipIt();

    } finally {
      pomDependenciesFileHandler.close();

      mavenLogger.logInfo(artifact, "END Main Artifact");
    }
  }

  private void processMainArtifact() throws IOException {
    mavenLogger.logInfo(artifact, "Processing main artifact");

    Artifact artifactFromLocalRepo = mavenSession.getLocalRepository().find(artifact);
    if (!isArtifactAvailableToProcess(artifact, artifactFromLocalRepo)) {
      return;
    }
    processedArtifacts.add(artifactFromLocalRepo);
    pomDependenciesFileHandler.writeArtifactDependency(artifactFromLocalRepo);

    ArtifactProcessor artifactProcessor = ArtifactProcessorFactory.getArtifactProcessor(artifactFromLocalRepo,
            dependencyBuildDirectory, artifactHandler, mavenLogger, mavenSession);
    if (artifactProcessor == null) {
      mavenLogger.logError(artifactFromLocalRepo, "ArtifactProcessor not found. Returning.");
      return;
    }
    Model model = artifactProcessor.process();
    if (model != null) {
      Model mergedModel = mergeModelWithParents(model);

      Set<Artifact> dependencyArtifacts = getDependencyArtifacts(artifactFromLocalRepo, mergedModel);
      processDependencyArtifacts(dependencyArtifacts);
    }
  }

  private void processDependencyArtifacts(Set<Artifact> dependencyArtifacts) throws IOException {
    for (Artifact dependencyArtifact : dependencyArtifacts) {
      mavenLogger.logInfo(dependencyArtifact, "Processing dependent artifact");

      Artifact dependencyArtifactFromLocalRepo = mavenSession.getLocalRepository().find(dependencyArtifact);
      if (!isArtifactAvailableToProcess(dependencyArtifact, dependencyArtifactFromLocalRepo)) {
        return;
      }
      processedArtifacts.add(dependencyArtifact);
      pomDependenciesFileHandler.writeArtifactDependency(dependencyArtifactFromLocalRepo);

      ArtifactProcessor artifactProcessor = ArtifactProcessorFactory.getArtifactProcessor(dependencyArtifactFromLocalRepo,
              dependencyBuildDirectory, artifactHandler, mavenLogger, mavenSession);
      if (artifactProcessor == null) {
        mavenLogger.logError(dependencyArtifactFromLocalRepo, "ArtifactProcessor not found. Returning.");
        return;
      }
      Model model = artifactProcessor.process();
      if (model != null) {
        Model mergedModel = mergeModelWithParents(model);

        // yes, this is recursive
        Set<Artifact> subDependencyArtifacts = getDependencyArtifacts(dependencyArtifactFromLocalRepo, mergedModel);
        processDependencyArtifacts(subDependencyArtifacts);
      }
    }
  }

  private boolean isArtifactAvailableToProcess(Artifact arty2, Artifact artifactFromLocalRepo) {
    if (artifactFromLocalRepo == null) {
      mavenLogger.logError(arty2, "Not found in local repository");
      return false;
    }
    if (processedArtifacts.contains(artifactFromLocalRepo)) {
      mavenLogger.logInfo(arty2, "Already processed");
      return false;
    }
    return true;
  }

  private Set<Artifact> getDependencyArtifacts(final Artifact arty2, final Model model) {
    return Sets.newHashSet(Iterables.transform(model.getDependencies(), new Function<Dependency, Artifact>() {
      @Override
      public Artifact apply(Dependency dependency) {
        mavenLogger.logInfo(dependency, "Converting Dependency to Artifact");
        try {
          String resolvedVersion = resolveVersionIfNecessary(dependency, model);
          Artifact dependencyArtifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), resolvedVersion,
                  dependency.getScope(), dependency.getType(), dependency.getClassifier(), artifactHandler);
          dependencyArtifact.setOptional(dependency.isOptional());
          mavenLogger.logInfo(arty2, dependencyArtifact, "");
          return dependencyArtifact;
        } catch (IOException e) {
          mavenLogger.logError(arty2, e.getMessage(), e);
          throw new RuntimeException(e);
        }
      }
    }));
  }

  private String resolveVersionIfNecessary(final Dependency dependency, final Model model) throws IOException {
    if (dependency.getVersion() == null) {
      mavenLogger.logInfo(artifact, "Resolving 'null' version. Checking dependencyManagement.");

      // try to resolve from dependencyManagement
      List<Dependency> managedDependencies = Lists.newArrayList();
      if (model.getDependencyManagement() != null && model.getDependencyManagement().getDependencies() != null) {
        managedDependencies.addAll(model.getDependencyManagement().getDependencies());
      }
      Dependency managedDependency = Iterables.find(managedDependencies, new Predicate<Dependency>() {
        @Override
        public boolean apply(Dependency input) {
          return dependency.getGroupId().equals(input.getGroupId()) && dependency.getArtifactId().equals(input.getArtifactId());
        }
      }, null);
      // I'm sure we can still get a null here
      mavenLogger.logInfo(artifact, String.format("Resolved version '%s'", managedDependency.getVersion()));
      return managedDependency.getVersion();
    } else if (dependency.getVersion().startsWith("${") && dependency.getVersion().endsWith("}")) {
      mavenLogger.logInfo(artifact, String.format("Resolving property version: '%s'", dependency.getVersion()));
      String versionPropertyKey = dependency.getVersion().substring(2, dependency.getVersion().length() - 1);
      String resolvedPropertyVersion = resolvePropertyVersion(versionPropertyKey, model);
      mavenLogger.logInfo(artifact, String.format("Resolved version '%s'", resolvedPropertyVersion));
      return resolvedPropertyVersion;
    }
    return dependency.getVersion();
  }

  private String resolvePropertyVersion(final String versionPropertyKey, final Model model) {
    if ("project.version".equals(versionPropertyKey)) {
      return model.getVersion() != null ? model.getVersion() : model.getParent().getVersion();
    } else {
      mavenLogger.dumpModelProperties(model);
      return model.getProperties().getProperty(versionPropertyKey);
    }
  }

  private Model mergeModelWithParents(final Model model) throws IOException {
    ModelMerger modelMerger = new ModelMerger();

    Model originalModel = model.clone();
    Model parentModel = getParentModel(model);

    Model mergedModel = model.clone();

    while (parentModel != null) {
      mergeModels(mergedModel, parentModel, modelMerger);

      parentModel = getParentModel(parentModel);
    }

    originalModel.setDependencies(mergedModel.getDependencies());
    originalModel.setDependencyManagement(mergedModel.getDependencyManagement());
    originalModel.setProperties(mergedModel.getProperties());

    return originalModel;
  }

  private Model getParentModel(final Model model) throws IOException {
    Parent parent = model.getParent();
    if (parent != null) {
      Artifact parentArtifact = new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(),
              null, "pom", null, artifactHandler);
      Artifact parentArtifactFromLocalRepo = mavenSession.getLocalRepository().find(parentArtifact);
      File parentPomFile = new File(parentArtifactFromLocalRepo.getFile() + ".pom");
      return parentPomFile.exists() && parentPomFile.isFile() ? loadModel(parentPomFile) : null;
    } else {
      return null;
    }
  }

  private Model loadModel(File pomFile) throws IOException {
    DefaultModelReader modelReader = new DefaultModelReader();
    return modelReader.read(pomFile, Maps.<String, Object>newHashMap());
  }

  private void mergeModels(Model modelTarget, Model modelSource, final ModelMerger modelMerger) {
//    if (modelTarget != null) {
    modelMerger.merge(modelTarget, modelSource, true, Maps.newHashMap());
//    }
  }

}
