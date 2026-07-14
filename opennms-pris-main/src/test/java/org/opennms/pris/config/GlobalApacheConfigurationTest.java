/*
 * Copyright 2026 The OpenNMS Group, Inc.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Created by Ronny Trommer <ronny@opennms.com>
 */
package org.opennms.pris.config;

import static org.hamcrest.MatcherAssert.assertThat;
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
 * Characterization tests for the global configuration and its merge into
 * instance configurations. The 2.x migration (PRIS-181) must keep every one
 * of these green.
 */
public class GlobalApacheConfigurationTest {

    private static final String SYS_KEY = "pris.test.syskey";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path base;

    @Before
    public void setUp() throws IOException {
        base = folder.getRoot().toPath();
        Files.write(base.resolve("global.properties"), String.join("\n",
                "shared = from-global",
                "onlyglobal = global-only",
                SYS_KEY + " = from-file")
                .getBytes(StandardCharsets.ISO_8859_1));

        final Path instanceDir = Files.createDirectories(base.resolve("requisitions").resolve("myreq"));
        Files.write(instanceDir.resolve("requisition.properties"), String.join("\n",
                "source = xls",
                "shared = from-instance")
                .getBytes(StandardCharsets.ISO_8859_1));

        System.setProperty(SYS_KEY, "from-system");
        System.setProperty("pris.config", base.toString());
    }

    @After
    public void tearDown() {
        System.clearProperty(SYS_KEY);
        System.clearProperty("pris.config");
    }

    @Test
    public void missingGlobalPropertiesIsToleratedAsEmptyConfig() throws IOException {
        final Path empty = folder.newFolder("empty").toPath();
        final GlobalApacheConfiguration config = new GlobalApacheConfiguration(empty);
        assertThat(config.containsKey("onlyglobal"), is(false));
        assertThat(config.getString("absent", "fallback"), is("fallback"));
        // system properties remain visible even without a global.properties
        assertThat(config.getString(SYS_KEY), is("from-system"));
    }

    @Test
    public void systemPropertyWinsOverGlobalPropertiesFile() {
        assertThat(new GlobalApacheConfiguration(base).getString(SYS_KEY), is("from-system"));
    }

    @Test
    public void instanceValueWinsOverGlobalValue() {
        final InstanceConfiguration config = new ConfigManager().getInstanceConfigWithGlobals("myreq");
        assertThat(config.getString("shared"), is("from-instance"));
    }

    @Test
    public void globalValueFillsMissingInstanceKey() {
        final InstanceConfiguration config = new ConfigManager().getInstanceConfigWithGlobals("myreq");
        assertThat(config.getString("onlyglobal"), is("global-only"));
    }

    @Test
    public void instanceNameIsExposedAsRequisitionProperty() {
        final InstanceConfiguration config = new ConfigManager().getInstanceConfigWithGlobals("myreq");
        assertThat(config.getString("requisition"), is("myreq"));
    }
}
