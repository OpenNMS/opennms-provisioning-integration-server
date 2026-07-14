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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opennms.pris.ConfigManager;
import org.opennms.pris.api.InstanceConfiguration;

/**
 * Characterization tests for the merge performed by
 * {@code ConfigManager.getInstanceConfigWithGlobals}, where the global
 * {@code CompositeConfiguration} (system properties layered over
 * global.properties) is copied key by key into the instance configuration.
 *
 * The merge copies values through {@code getString}/{@code addProperty}, so it
 * is the one place where the list delimiter handling of both configurations
 * meets. The 2.x migration (PRIS-181) must keep every one of these green,
 * including the lossy cases pinned down below.
 */
public class ConfigManagerGlobalsMergeTest {

    private static final String SYS_ONLY_KEY = "pris.test.sysonly";

    private static final String SYS_OVER_INSTANCE_KEY = "pris.test.instancewins";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path base;

    private Path instanceDir;

    @Before
    public void setUp() throws IOException {
        base = folder.getRoot().toPath();
        Files.write(base.resolve("global.properties"), String.join("\n",
                "gtags = alpha, beta",
                "glabel = a\\, b",
                "gfile = steps.groovy",
                "gscripts = one.groovy, two.groovy",
                SYS_ONLY_KEY + " = from-file")
                .getBytes(StandardCharsets.ISO_8859_1));

        instanceDir = Files.createDirectories(base.resolve("requisitions").resolve("myreq"));
        Files.write(instanceDir.resolve("requisition.properties"), String.join("\n",
                "source = xls",
                SYS_OVER_INSTANCE_KEY + " = from-instance")
                .getBytes(StandardCharsets.ISO_8859_1));

        System.setProperty(SYS_ONLY_KEY, "from-system");
        System.setProperty(SYS_OVER_INSTANCE_KEY, "from-system");
        System.setProperty("pris.config", base.toString());
    }

    @After
    public void tearDown() {
        System.clearProperty(SYS_ONLY_KEY);
        System.clearProperty(SYS_OVER_INSTANCE_KEY);
        System.clearProperty("pris.config");
    }

    private InstanceConfiguration merged() {
        return new ConfigManager().getInstanceConfigWithGlobals("myreq");
    }

    @Test
    public void systemPropertyReachesTheInstanceConfigThroughTheMerge() {
        // The system layer of the CompositeConfiguration is visible on the
        // merged instance config, not just on the global config itself
        assertThat(merged().getString(SYS_ONLY_KEY), is("from-system"));
    }

    @Test
    public void systemPropertyWinsOverGlobalFileThroughTheMerge() {
        // SYS_ONLY_KEY is set in both global.properties and the system
        // properties - the composite adds the system config first, so the
        // file value never reaches the instance config
        assertThat(new GlobalApacheConfiguration(base).getString(SYS_ONLY_KEY), is("from-system"));
        assertThat(merged().getString(SYS_ONLY_KEY), is("from-system"));
    }

    @Test
    public void instanceValueWinsOverSystemProperty() {
        // Completes the precedence chain instance > system > global.properties:
        // the merge skips keys the instance already defines
        assertThat(merged().getString(SYS_OVER_INSTANCE_KEY), is("from-instance"));
    }

    @Test
    public void multiValueGlobalKeySurvivesTheMerge() {
        // A plain comma list round-trips through the join-on-read,
        // split-on-write of the merge
        assertThat(merged().getStringArray("gtags"), arrayContaining("alpha", "beta"));
        assertThat(merged().getString("gtags"), is("alpha,beta"));
    }

    @Test
    public void escapedCommaInGlobalValueIsSplitByTheMerge() {
        // The global config reads the escaped comma as a single value ...
        assertThat(new GlobalApacheConfiguration(base).getStringArray("glabel"),
                   arrayContaining("a, b"));

        // ... but the merge copies it as the joined string "a, b" and the
        // instance config splits it again on the delimiter, dropping the
        // escape. Pre-existing lossy behavior of the key-by-key copy, kept
        // as-is by the 2.x migration.
        assertThat(merged().getStringArray("glabel"), arrayContaining("a", "b"));
        assertThat(merged().getString("glabel"), is("a,b"));
    }

    @Test
    public void globalPathValueResolvesAgainstTheInstanceDirectory() {
        // Path-valued globals are resolved by the instance config, so they
        // resolve against the instance folder - not against the config base
        // folder the value was written in
        assertThat(merged().getPath("gfile"), is(instanceDir.resolve("steps.groovy")));
        assertThat(merged().getPaths("gscripts"), contains(instanceDir.resolve("one.groovy"),
                                                           instanceDir.resolve("two.groovy")));
    }

    @Test
    public void presetRequisitionKeyGainsTheInstanceNameAsSecondValue() throws IOException {
        // The merge adds the instance name as the "requisition" property
        // unconditionally, so a requisition.properties that already sets the
        // key ends up holding two values instead of being overwritten
        final Path preset = Files.createDirectories(base.resolve("requisitions").resolve("preset"));
        Files.write(preset.resolve("requisition.properties"),
                    "requisition = other".getBytes(StandardCharsets.ISO_8859_1));

        final InstanceConfiguration config = new ConfigManager().getInstanceConfigWithGlobals("preset");
        assertThat(config.getStringArray("requisition"), arrayContaining("other", "preset"));
        assertThat(config.getString("requisition"), is("other,preset"));
    }
}
