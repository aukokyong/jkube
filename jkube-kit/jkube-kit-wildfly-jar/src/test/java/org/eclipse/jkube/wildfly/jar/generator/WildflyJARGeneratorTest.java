/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.wildfly.jar.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyJARGenerator.JBOSS_MAVEN_DIST;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyJARGenerator.JBOSS_MAVEN_REPO;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyJARGenerator.PLUGIN_OPTIONS;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

/**
 * @author roland
 */

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class WildflyJARGeneratorTest {

    @Mocked
    private GeneratorContext context;

    @Mocked
    private JavaProject project;

    @Test
    public void notApplicable() throws IOException {
        WildflyJARGenerator generator = new WildflyJARGenerator(createGeneratorContext());
        assertThat(generator.isApplicable(Collections.emptyList())).isFalse();
    }

    // To be revisited if we enable jolokia and prometheus.
    @Test
    public void getEnv() throws IOException {
        WildflyJARGenerator generator = new WildflyJARGenerator(createGeneratorContext());
        Map<String, String> extraEnv = generator.getEnv(true);
        assertThat(extraEnv).isNotNull().hasSize(4);
    }
    
    @Test
    public void getExtraOptions() throws IOException {
        WildflyJARGenerator generator = new WildflyJARGenerator(createGeneratorContext());
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull()
                .singleElement().isEqualTo("-Djava.net.preferIPv4Stack=true");
    }
    
    @Test
    public void slimServer(@Mocked final JavaProject project) throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_DIST, null);
        pluginOptions.put(JBOSS_MAVEN_REPO, "target" + File.separator + "myrepo");
        //
        Path tmpDir = Files.createTempDirectory("bootable-jar-test-project");
        Path targetDir = tmpDir.resolve("target");
        Path repoDir = targetDir.resolve("myrepo");
        Files.createDirectories(repoDir);
        try {
            GeneratorContext ctx = contextForSlimServer(project, options, tmpDir);
            WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
            List<String> extraOptions = generator.getExtraJavaOptions();
            assertThat(extraOptions).isNotNull()
                    .hasSize(2)
                    .first().isEqualTo("-Djava.net.preferIPv4Stack=true");
            assertThat(extraOptions.get(1)).isEqualTo("-Dmaven.repo.local=/deployments/myrepo");
            List<AssemblyFileSet> files = generator.addAdditionalFiles();
            assertThat(files).isNotEmpty();
            AssemblyFileSet set = files.get(files.size() - 1);
            assertThat(set.getDirectory()).isEqualTo(targetDir.toFile());
            assertThat(set.getIncludes()).singleElement().isEqualTo("myrepo");
        } finally {
            Files.delete(repoDir);
            Files.delete(targetDir);
            Files.delete(tmpDir);
        }
    }
    
    @Test
    public void slimServerAbsoluteDir(@Mocked final JavaProject project) throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        Path tmpDir = Files.createTempDirectory("bootable-jar-test-project2");
        Path targetDir = tmpDir.resolve("target");
        Path repoDir = targetDir.resolve("myrepo");
        Files.createDirectories(repoDir);
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_DIST, null);
        pluginOptions.put(JBOSS_MAVEN_REPO, repoDir.toString());
        try {
            GeneratorContext ctx = contextForSlimServer(project, options, null);
            WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
            List<String> extraOptions = generator.getExtraJavaOptions();
            assertThat(extraOptions).isNotNull().hasSize(2)
                    .first().isEqualTo("-Djava.net.preferIPv4Stack=true");
            assertThat(extraOptions.get(1)).isEqualTo("-Dmaven.repo.local=/deployments/myrepo");
            List<AssemblyFileSet> files = generator.addAdditionalFiles();
            assertThat(files).isNotEmpty();
            AssemblyFileSet set = files.get(files.size() - 1);
            assertThat(set.getDirectory()).isEqualTo(targetDir.toFile());
            assertThat(set.getIncludes()).singleElement().isEqualTo("myrepo");
        } finally {
            Files.delete(repoDir);
            Files.delete(targetDir);
            Files.delete(tmpDir);
        }
    }
    
    @Test
    public void slimServerNoDir(@Mocked final JavaProject project) throws Exception {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        Path tmpDir = Files.createTempDirectory("bootable-jar-test-project2");
        Path targetDir = tmpDir.resolve("target");
        Path repoDir = targetDir.resolve("myrepo");
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_DIST, null);
        pluginOptions.put(JBOSS_MAVEN_REPO, repoDir.toString());
        try {
            GeneratorContext ctx = contextForSlimServer(project, options, null);
            WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
            List<String> extraOptions = generator.getExtraJavaOptions();
            assertThat(extraOptions).isNotNull().hasSize(2)
                    .first().isEqualTo("-Djava.net.preferIPv4Stack=true");
            assertThat(extraOptions.get(1)).isEqualTo("-Dmaven.repo.local=/deployments/myrepo");
            Exception result = assertThrows(Exception.class, () -> {
                generator.addAdditionalFiles();
                fail("Test should have failed, no directory for maven repo");
            });
            assertThat(result).hasMessage("Error, WildFly bootable JAR generator can't retrieve "
                    + "generated maven local cache, directory " + repoDir + " doesn't exist.");
        } finally {
            Files.delete(tmpDir);
        }
    }
    
    @Test
    public void slimServerNoRepo(@Mocked final JavaProject project) {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_DIST, null);
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull()
                .singleElement().isEqualTo("-Djava.net.preferIPv4Stack=true");
    }
    
    @Test
    public void slimServerNoDist(@Mocked final JavaProject project) {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_REPO, "myrepo");
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull().singleElement().isEqualTo("-Djava.net.preferIPv4Stack=true");
    }
    
    @Test
    public void slimServerFalseDist(@Mocked final JavaProject project) {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_REPO, "myrepo");
        pluginOptions.put(JBOSS_MAVEN_DIST, "false");
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull().singleElement().isEqualTo("-Djava.net.preferIPv4Stack=true");
    }
    
    @Test
    public void slimServerTrueDist(@Mocked final JavaProject project) {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_REPO, "myrepo");
        pluginOptions.put(JBOSS_MAVEN_DIST, "true");
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull().hasSize(2)
                .first().isEqualTo("-Djava.net.preferIPv4Stack=true");
        assertThat(extraOptions.get(1)).isEqualTo("-Dmaven.repo.local=/deployments/myrepo");
    }
    
    private GeneratorContext contextForSlimServer(JavaProject project, Map<String, Object> bootableJarconfig, Path dir) {
        Plugin plugin
                = Plugin.builder().artifactId("wildfly-jar-maven-plugin").
                        groupId("org.wildfly.plugins").configuration(bootableJarconfig).build();
        List<Plugin> lst = new ArrayList<>();
        lst.add(plugin);
        ProcessorConfig c = new ProcessorConfig(null, null, Collections.emptyMap());
        if (dir == null) {
            new Expectations() {
                {
                    project.getPlugins();
                    result = lst;
                    context.getProject();
                    result = project;
                }
            };
        } else {
            new Expectations() {
                {
                    project.getPlugins();
                    result = lst;
                    project.getBaseDirectory();
                    result = dir.toFile();
                    context.getProject();
                    result = project;
                }
            };
        }
        return context;
    }

    private GeneratorContext createGeneratorContext() throws IOException {
        new Expectations() {{
            context.getProject(); result = project;
            String tempDir = Files.createTempDirectory("wildfly-jar-test-project").toFile().getAbsolutePath();
            project.getOutputDirectory(); result = tempDir;
            project.getPlugins(); result = Collections.emptyList(); minTimes = 0;
            project.getVersion(); result = "1.0.0"; minTimes = 0;
        }};
        return context;
    }
}
