/*
 * Copyright 2026 The OpenNMS Group, Inc.
 *
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Created by Ronny Trommer <ronny@opennms.com>
 */

package org.opennms.pris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests that instance discovery ({@code getInstances}) and instance loading ({@code getInstanceConfig})
 * agree on the {@literal requisitions} folder as the location of instance configurations.
 */
public class ConfigManagerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path base;

    private String oldConfigProperty;

    @Before
    public void setUp() throws IOException {
        this.base = this.tempFolder.getRoot().toPath();

        // ConfigManager requires a global.properties in the config base folder
        Files.createFile(this.base.resolve("global.properties"));

        this.oldConfigProperty = System.setProperty("pris.config", this.base.toString());
    }

    @After
    public void tearDown() {
        if (this.oldConfigProperty == null) {
            System.clearProperty("pris.config");
        } else {
            System.setProperty("pris.config", this.oldConfigProperty);
        }
    }

    private void createInstance(final String name) throws IOException {
        final Path instanceFolder = this.base.resolve("requisitions").resolve(name);
        Files.createDirectories(instanceFolder);
        Files.write(instanceFolder.resolve("requisition.properties"),
                    Arrays.asList("source = file", "mapper = echo"));
    }

    @Test
    public void discoversInstancesInRequisitionsFolder() throws IOException {
        this.createInstance("alpha");
        this.createInstance("beta");

        // A folder without a requisition.properties is not an instance
        Files.createDirectories(this.base.resolve("requisitions").resolve("no-instance"));

        // A plain file in the requisitions folder is not an instance
        Files.createFile(this.base.resolve("requisitions").resolve("inventory.xls"));

        final Collection<String> instances = new ConfigManager().getInstances();

        assertEquals(new HashSet<>(Arrays.asList("alpha", "beta")), new HashSet<>(instances));
    }

    @Test
    public void filtersInstancesByGlob() throws IOException {
        this.createInstance("myRouter");
        this.createInstance("myServer");
        this.createInstance("other");

        final Collection<String> instances = new ConfigManager().getInstances("my*");

        assertEquals(new HashSet<>(Arrays.asList("myRouter", "myServer")), new HashSet<>(instances));
    }

    @Test
    public void missingRequisitionsFolderYieldsNoInstances() {
        final Collection<String> instances = new ConfigManager().getInstances();

        assertTrue("expected no instances without a requisitions folder", instances.isEmpty());
    }

    @Test
    public void discoveredInstancesAreLoadable() throws IOException {
        this.createInstance("alpha");

        final ConfigManager configManager = new ConfigManager();

        // Every discovered instance must be loadable by getInstanceConfig - this pins
        // discovery and loading to the same folder
        for (final String instance : configManager.getInstances()) {
            assertEquals("file", configManager.getInstanceConfig(instance).getString("source"));
        }
    }
}
