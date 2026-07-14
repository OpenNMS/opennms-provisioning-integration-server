/*
 * Copyright 2026 The OpenNMS Group, Inc.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Created by Ronny Trommer <ronny@opennms.com>
 */
package org.opennms.pris.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opennms.pris.api.InstanceConfiguration;

/**
 * Characterization tests pinning down the observable behavior of the
 * Commons Configuration backed instance configuration. The 2.x migration
 * (PRIS-181) must keep every one of these green.
 */
public class InstanceApacheConfigurationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path instanceDir;

    @Before
    public void setUp() throws IOException {
        instanceDir = folder.newFolder("myreq").toPath();
        Files.write(instanceDir.resolve("requisition.properties"), String.join("\n",
                "source = xls",
                "port = 8000",
                "flag = true",
                "tags = alpha, beta",
                "label = a\\, b",
                "file = steps.groovy",
                "scripts = one.groovy, two.groovy",
                "mapper.script.file = mapper.groovy")
                .getBytes(StandardCharsets.ISO_8859_1));
    }

    private InstanceConfiguration config() {
        return new InstanceApacheConfiguration(instanceDir, "myreq");
    }

    @Test
    public void loadsPropertiesFromInstanceDirectory() {
        assertThat(config().getString("source"), is("xls"));
        assertThat(config().containsKey("source"), is(true));
        assertThat(config().isEmpty(), is(false));
        assertThat(config().getInstanceIdentifier(), is("myreq"));
    }

    @Test
    public void missingConfigFileFailsNamingThePath() throws IOException {
        final Path empty = folder.newFolder("empty").toPath();
        try {
            new InstanceApacheConfiguration(empty, "empty");
            throw new AssertionError("expected a RuntimeException for the missing config file");
        } catch (final RuntimeException ex) {
            assertThat(ex.getMessage(), containsString("Config file not found"));
            assertThat(ex.getMessage(), containsString("requisition.properties"));
        }
    }

    @Test
    public void typedAccessorsReturnConfiguredValues() {
        assertThat(config().getInt("port"), is(8000));
        assertThat(config().getBoolean("flag"), is(true));
    }

    @Test
    public void defaultsApplyForMissingKeys() {
        assertThat(config().getString("absent", "fallback"), is("fallback"));
        assertThat(config().getInt("absent", 42), is(42));
        assertThat(config().getBoolean("absent", true), is(true));
        assertThat(config().getPath("absent", instanceDir), is(instanceDir));
    }

    @Test(expected = NoSuchElementException.class)
    public void missingKeyWithoutDefaultThrowsOnGetString() {
        config().getString("absent");
    }

    @Test(expected = NoSuchElementException.class)
    public void missingKeyWithoutDefaultThrowsOnGetInt() {
        config().getInt("absent");
    }

    @Test
    public void commaSeparatedValueSplitsIntoArray() {
        assertThat(config().getStringArray("tags"), arrayContaining("alpha", "beta"));
    }

    @Test
    public void commaSeparatedValueJoinsBackOnGetString() {
        assertThat(config().getString("tags"), is("alpha,beta"));
    }

    @Test
    public void escapedCommaStaysASingleValue() {
        assertThat(config().getStringArray("label"), arrayContaining("a, b"));
    }

    @Test
    public void pathResolvesAgainstInstanceDirectory() {
        assertThat(config().getPath("file"), is(instanceDir.resolve("steps.groovy")));
    }

    @Test
    public void pathsResolveAgainstInstanceDirectoryInOrder() {
        final List<Path> paths = config().getPaths("scripts");
        assertThat(paths, contains(instanceDir.resolve("one.groovy"),
                                   instanceDir.resolve("two.groovy")));
    }

    @Test
    public void subsetStripsThePrefixAndKeepsIdentity() {
        final InstanceConfiguration subset = config().subset("mapper");
        assertThat(subset.getString("script.file"), is("mapper.groovy"));
        assertThat(subset.containsKey("script.file"), is(true));
        assertThat(subset.getInstanceIdentifier(), is("myreq"));
        assertThat(subset.getBasePath(), is(instanceDir));
        assertThat(subset.getPath("script.file"), is(instanceDir.resolve("mapper.groovy")));
    }
}
