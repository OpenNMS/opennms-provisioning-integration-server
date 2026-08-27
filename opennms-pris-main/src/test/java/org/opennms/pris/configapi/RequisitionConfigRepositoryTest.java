/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2026 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2026 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.pris.configapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RequisitionConfigRepositoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path requisitions;
    private RequisitionConfigRepository repository;

    @Before
    public void setUp() throws IOException {
        this.requisitions = this.folder.newFolder("requisitions").toPath();
        this.repository = new RequisitionConfigRepository(this.requisitions);
    }

    @Test
    public void writeAndReadRoundTrip() throws IOException {
        this.repository.write("myTest", Map.of("source", "file",
                                               "source.file", "/tmp/some.xml",
                                               "mapper", "echo"));

        final Map<String, String> read = this.repository.read("myTest");
        assertEquals("file", read.get("source"));
        assertEquals("/tmp/some.xml", read.get("source.file"));
        assertEquals("echo", read.get("mapper"));
        assertEquals(3, read.size());
    }

    @Test
    public void writeEscapesSpecialCharactersSymmetrically() throws IOException {
        this.repository.write("myTest", Map.of("source", "file",
                                               "source.file", "C:\\data\\inventory.xls",
                                               "note", "first\nsecond",
                                               "unicode", "priorité €"));

        final Map<String, String> read = this.repository.read("myTest");
        assertEquals("C:\\data\\inventory.xls", read.get("source.file"));
        assertEquals("first\nsecond", read.get("note"));
        assertEquals("priorité €", read.get("unicode"));
        assertEquals(4, read.size());
    }

    @Test
    public void writeReplacesExistingConfiguration() throws IOException {
        this.repository.write("myTest", Map.of("source", "file", "mapper", "echo"));
        this.repository.write("myTest", Map.of("source", "http", "source.url", "http://example.org"));

        final Map<String, String> read = this.repository.read("myTest");
        assertEquals("http", read.get("source"));
        assertFalse(read.containsKey("mapper"));
    }

    @Test
    public void writeLeavesNoTemporaryFiles() throws IOException {
        this.repository.write("myTest", Map.of("source", "file"));

        try (final Stream<Path> files = Files.list(this.requisitions.resolve("myTest"))) {
            assertThat(files.map(p -> p.getFileName().toString()).toList(),
                       contains("requisition.properties"));
        }
    }

    @Test
    public void listReturnsOnlyRealRequisitions() throws IOException {
        this.repository.write("bravo", Map.of("source", "file"));
        this.repository.write("alpha", Map.of("source", "file"));

        // A stray file and a folder without properties file are not requisitions
        Files.createFile(this.requisitions.resolve("stray.xls"));
        Files.createDirectory(this.requisitions.resolve("empty"));

        assertThat(this.repository.list(), contains("alpha", "bravo"));
    }

    @Test
    public void listToleratesMissingRequisitionsFolder() {
        final RequisitionConfigRepository other =
                new RequisitionConfigRepository(this.requisitions.resolve("does-not-exist"));

        assertThat(other.list(), is(empty()));
    }

    @Test
    public void deleteRemovesRequisitionWithContainedFiles() throws IOException {
        this.repository.write("myTest", Map.of("source", "file"));
        Files.createFile(this.requisitions.resolve("myTest").resolve("script.groovy"));

        assertTrue(this.repository.delete("myTest"));
        assertFalse(Files.exists(this.requisitions.resolve("myTest")));
    }

    @Test
    public void deleteReturnsFalseForUnknownRequisition() throws IOException {
        assertFalse(this.repository.delete("unknown"));
    }

    @Test
    public void readUnknownRequisitionThrows() {
        assertThrows(NoSuchFileException.class, () -> this.repository.read("unknown"));
    }

    @Test
    public void namesWithTraversalAttemptsAreRejected() {
        assertFalse(RequisitionConfigRepository.isValidName(null));
        assertFalse(RequisitionConfigRepository.isValidName(""));
        assertFalse(RequisitionConfigRepository.isValidName(".."));
        assertFalse(RequisitionConfigRepository.isValidName("../evil"));
        assertFalse(RequisitionConfigRepository.isValidName("a/b"));
        assertFalse(RequisitionConfigRepository.isValidName("a\\b"));
        assertFalse(RequisitionConfigRepository.isValidName(".hidden"));

        assertTrue(RequisitionConfigRepository.isValidName("myServer"));
        assertTrue(RequisitionConfigRepository.isValidName("site-1.example_net"));

        assertThrows(IllegalArgumentException.class,
                     () -> this.repository.write("../evil", Map.of("source", "file")));
        assertThrows(IllegalArgumentException.class,
                     () -> this.repository.read("../evil"));
        assertThrows(IllegalArgumentException.class,
                     () -> this.repository.delete("../evil"));
    }
}
