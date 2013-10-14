package com.jtl.maven.plugins.artifactlicensedeps;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.merge.ModelMerger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Mojo(name = "process-artifacts", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class ProcessArtifactsMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.directory}/artifacts", required = true, readonly = true)
  private File buildDirectory;

  @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
  private String encoding;

  @Parameter(property = "maven.install.skip", defaultValue = "false", required = true)
  private boolean skip;

  @Component
  private ArtifactHandler artifactHandler;

  @Component
  private MavenProject project;

  @Component
  protected MavenSession mavenSession;

//  @Component
//  private MojoExecution mojo;

  @Component
  private Settings settings;

//  private Set<Artifact> processedArtifacts = Sets.newHashSet();

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!skip) {
      try {
        getLog().info("START Project Artifact License and Dependency Processing");

        MavenLogger mavenLogger = new MavenLogger(getLog());

        Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();
        for (Artifact artifact : dependencyArtifacts) {
          MainArtifactProcessor mainArtifactProcessor = new MainArtifactProcessor(artifact, artifactHandler, buildDirectory, mavenLogger, mavenSession);
          mainArtifactProcessor.process();

//          logInfo(artifact, "Processing main artifact");
//
//          Set<Artifact> processedArtifacts = Sets.newHashSet();
//
//          String dependencyType = Objects.firstNonNull(artifact.getType(), "jar");
//          String dependencyBuildDirName = Files.simplifyPath(buildDirectory.getAbsolutePath()) + File.separator + artifact.getArtifactId() + "-" + artifact.getVersion() + "-" + dependencyType;
//          logInfo(artifact, "Creating build directory");
//
//          File dependencyBuildDirectory = new File(dependencyBuildDirName);
//          dependencyBuildDirectory.mkdirs();
//
//          String pomDependencyFileName = Files.simplifyPath(dependencyBuildDirectory.getAbsolutePath()) + File.separator + artifact.getArtifactId() + "-" + artifact.getVersion() + "-DEPENDENCIES-POM.txt";
//          File pomDependenciesFile = new File(pomDependencyFileName);
//          BufferedWriter pomDependenciesFileWriter = Files.newWriter(pomDependenciesFile, Charset.defaultCharset());
//
//          pomDependenciesFileWriter.write(String.format("'%s:%s:%s' : POM Derived Artifact Dependencies", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
//          pomDependenciesFileWriter.newLine();
//          pomDependenciesFileWriter.newLine();
//
//          processProjectDependencyArtifact(artifact, dependencyBuildDirectory, pomDependenciesFileWriter, processedArtifacts);
//
//          pomDependenciesFileWriter.close();
//
//          // zip dependencyBuildDirFile
//          File zipFile = new File(dependencyBuildDirName + ".zip");
//          new DirectoryZipper(zipFile, dependencyBuildDirectory).zipIt();
        }
      } catch (Exception e) {
        String exceptionMessage = String.format("Exception During Project Artifact and Dependency Processing: '%s'", e.getMessage());
        getLog().error(exceptionMessage, e);
        throw new MojoExecutionException(exceptionMessage, e);
      } finally {
        getLog().info("END Project Artifact License and Dependency Processing");
      }
    }
  }

//  private void processProjectDependencyArtifact(Artifact artifact, File dependencyBuildDirectory,
//                                                BufferedWriter pomDependenciesFileWriter, Set<Artifact> processedArtifacts) throws IOException {
//    Artifact artifactFromLocalRepo = mavenSession.getLocalRepository().find(artifact);
//    if (!isArtifactAvailableToProcess(artifact, artifactFromLocalRepo, processedArtifacts)) {
//      return;
//    }
//    processedArtifacts.add(artifactFromLocalRepo);
//    pomDependenciesFileWriter.write(String.format("%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
//    pomDependenciesFileWriter.newLine();
//
//    ArtifactProcessor artifactProcessor = ArtifactProcessorFactory.getArtifactProcessor(artifactFromLocalRepo, dependencyBuildDirectory, artifactHandler, mavenSession);
//    if (artifactProcessor == null) {
//      getLog().error(String.format("ArtifactProcessor not found for artifact '%s:%s:%s:%s'", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType()));
//      return;
//    }
//    Model model = artifactProcessor.process();
//
//    Set<Artifact> artifactDependencies = toArtifacts(model);
//    processDependencyArtifacts(artifactDependencies, dependencyBuildDirectory, pomDependenciesFileWriter, processedArtifacts);
//  }

//  private void processDependencyArtifacts(Set<Artifact> artifacts, File dependencyBuildDirectory,
//                                          BufferedWriter pomDependenciesFileWriter, Set<Artifact> processedArtifacts) throws IOException {
//    for (Artifact artifact : artifacts) {
//      getLog().info(String.format("Processing dependent artifact '%s:%s:%s'", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
//
//      Artifact artifactFromLocalRepo = mavenSession.getLocalRepository().find(artifact);
//      if (!isArtifactAvailableToProcess(artifact, artifactFromLocalRepo, processedArtifacts)) {
//        return;
//      }
//      processedArtifacts.add(artifactFromLocalRepo);
//      pomDependenciesFileWriter.write(String.format("%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
//      pomDependenciesFileWriter.newLine();
//
//      ArtifactProcessor artifactProcessor = ArtifactProcessorFactory.getArtifactProcessor(artifactFromLocalRepo, dependencyBuildDirectory, artifactHandler, mavenSession);
//      if (artifactProcessor == null) {
//        getLog().error(String.format("ArtifactProcessor not found for artifact '%s:%s:%s:%s'", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType()));
//        return;
//      }
//      Model model = artifactProcessor.process();
//
//      Set<Artifact> artifactDependencies = toArtifacts(model);
//      processDependencyArtifacts(artifactDependencies, dependencyBuildDirectory, pomDependenciesFileWriter, processedArtifacts);
//    }
//  }

//  private boolean isArtifactAvailableToProcess(Artifact artifact, Artifact artifactFromLocalRepo, Set<Artifact> processedArtifacts) {
//    if (artifactFromLocalRepo == null) {
//      getLog().warn(String.format("Could not find local repository for artifact '%s:%s:%s'", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
//      return false;
//    }
////    if ("org.apache.maven".equals(artifactFromLocalRepo.getGroupId())) {
////      getLog().info(String.format("Not processing '%s:%s:%s'", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
////      return false;
////    }
//    if (processedArtifacts.contains(artifactFromLocalRepo)) {
//      getLog().info(String.format("Already processed '%s:%s:%s'", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
//      return false;
//    }
//    return true;
//  }

  private boolean isArtifactInArtifactory(Artifact artifact) {
//    RemoteRepository central = new RemoteRepository("artifactory-central", null, "http://maven/libs-release");

    for (Mirror mirror : settings.getMirrors()) {

    }

    return false;
  }

//  private void dumpProperties(Model model) {
//    model.getProperties()
//  }

  private Set<Artifact> toArtifacts(final Model model) {
    return Sets.newHashSet(Iterables.transform(model.getDependencies(), new Function<Dependency, Artifact>() {
      @Override
      public Artifact apply(Dependency dependency) {
        logInfo(dependency, "Converting Dependency to Artifact");
        String resolvedVersion = null;

        try {
          resolvedVersion = resolveVersion(dependency, model);
        } catch (IOException e) {
          getLog().error(e);
          throw new RuntimeException(e);
        }

        Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), resolvedVersion,
                dependency.getScope(), dependency.getType(), dependency.getClassifier(), artifactHandler);
        artifact.setOptional(dependency.isOptional());
        return artifact;
      }
    }));
  }

  private String resolveVersion(final Dependency dependency, final Model model) throws IOException {
    if (dependency.getVersion() == null) {
      getLog().info(String.format("Resolving 'null' version"));
      // try to resolve from dependencyManagement
      List<Dependency> managedDependencies = Lists.newArrayList();
      mergeManagedDependencies(model, managedDependencies);
      Dependency managedDependency = Iterables.find(managedDependencies, new Predicate<Dependency>() {
        @Override
        public boolean apply(Dependency input) {
          return dependency.getGroupId().equals(input.getGroupId()) && dependency.getArtifactId().equals(input.getArtifactId());
        }
      }, null);
      getLog().info(String.format("Resolved version '%s'", managedDependency.getVersion()));
      return managedDependency.getVersion();
    } else if (dependency.getVersion().startsWith("${") && dependency.getVersion().endsWith("}")) {
      getLog().info(String.format("Resolving property version: '%s'", dependency.getVersion()));
      String versionPropertyKey = dependency.getVersion().substring(2, dependency.getVersion().length() - 1);
      getLog().info(String.format("Resolved version '%s'", model.getProperties().getProperty(versionPropertyKey)));
      return model.getProperties().getProperty(versionPropertyKey);
    }
    return dependency.getVersion();
  }

  private void mergeParentModels(final Model model) throws IOException {
    ModelMerger modelMerger = new ModelMerger();
    Parent currentParent = model.getParent();
    Model currentParentModel = model.clone();
    while (currentParent != null) {
      currentParentModel = getParentModel(currentParentModel);
      if (currentParentModel != null) {
        modelMerger.merge(currentParentModel, model, true, Maps.newHashMap());
        currentParent = currentParentModel.getParent();
      } else {
        currentParent = null;
      }
    }
  }

  private void mergeManagedDependencies(final Model model, final List<Dependency> managedDependencies) throws IOException {
    if (model.getDependencyManagement() != null) {
      managedDependencies.addAll(model.getDependencyManagement().getDependencies());
    }
    if (model.getParent() != null) {
      Model parentModel = getParentModel(model);
      if (parentModel != null) {
//        new ModelMerger().merge(parentModel, model, true, Maps.newHashMap());
        mergeManagedDependencies(parentModel, managedDependencies);
      }
    }
  }

  private Model getParentModel(final Model model) throws IOException {
    Parent parent = model.getParent();
    Artifact parentArtifact = new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(),
            null, "pom", null, artifactHandler);
    Artifact parentArtifactFromLocalRepo = mavenSession.getLocalRepository().find(parentArtifact);
    File parentPomFile = new File(parentArtifactFromLocalRepo.getFile() + ".pom");
    return loadModel(parentPomFile);
  }

  protected Model loadModel(File pomFile) throws IOException {
    DefaultModelReader modelReader = new DefaultModelReader();
    return modelReader.read(pomFile, Maps.<String, Object>newHashMap());
  }

  private void logInfo(Artifact artifact, String message) {
    getLog().info(String.format("%s '%s:%s:%s:%s'", message, artifact.getGroupId(), artifact.getArtifactId(),
            artifact.getVersion(), artifact.getType()));
  }

  private void logInfo(Dependency dependency, String message) {
    getLog().info(String.format("%s '%s:%s:%s:%s'", message, dependency.getGroupId(), dependency.getArtifactId(),
            dependency.getVersion(), dependency.getType()));
  }
}
