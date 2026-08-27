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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * File-backed store for requisition configurations.
 *
 * The property files under the requisitions folder stay the single source of
 * truth: this repository reads and writes the same
 * {@code requisitions/<name>/requisition.properties} files an operator edits
 * by hand, so both editing paths can be used interchangeably.
 */
public class RequisitionConfigRepository {

    /**
     * Allowed requisition names.
     *
     * The name doubles as a directory name below the requisitions folder, so
     * it must never contain path separators, and the leading character
     * excludes dot to rule out "." and "..".
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    private static final String PROPERTIES_FILE = "requisition.properties";

    // The folder holding one sub-folder per requisition
    private final Path requisitionsPath;

    public RequisitionConfigRepository(final Path requisitionsPath) {
        this.requisitionsPath = requisitionsPath;
    }

    /**
     * Checks if a requisition name is safe to use as a folder name.
     *
     * @param name the name to check
     *
     * @return {@code true} if the name is acceptable
     */
    public static boolean isValidName(final String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    /**
     * Lists the names of all configured requisitions.
     *
     * @return the sorted requisition names
     */
    public List<String> list() {
        if (!Files.isDirectory(this.requisitionsPath)) {
            return List.of();
        }

        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(this.requisitionsPath)) {
            final List<String> names = new ArrayList<>();
            for (final Path path : stream) {
                if (Files.isDirectory(path) && Files.exists(path.resolve(PROPERTIES_FILE))) {
                    names.add(path.getFileName().toString());
                }
            }

            names.sort(Comparator.naturalOrder());
            return names;

        } catch (final IOException ex) {
            throw new UncheckedIOException("Unable to list requisitions folder: " + this.requisitionsPath, ex);
        }
    }

    /**
     * Checks if a requisition configuration exists.
     *
     * @param name the requisition name
     *
     * @return {@code true} if the requisition has a properties file
     */
    public boolean exists(final String name) {
        return Files.exists(this.propertiesPath(name));
    }

    /**
     * Reads the configuration of a requisition.
     *
     * @param name the requisition name
     *
     * @return the properties as a sorted map
     *
     * @throws NoSuchFileException if the requisition does not exist
     * @throws IOException if the properties file can not be read
     */
    public Map<String, String> read(final String name) throws IOException {
        final Path path = this.propertiesPath(name);

        final Properties properties = new Properties();
        try (final InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        }

        final Map<String, String> result = new TreeMap<>();
        for (final String key : properties.stringPropertyNames()) {
            result.put(key, properties.getProperty(key));
        }

        return result;
    }

    /**
     * Creates or replaces the configuration of a requisition.
     *
     * The properties file is written to a temporary file first and moved into
     * place atomically, so a concurrent requisition request never sees a
     * partially written configuration.
     *
     * @param name the requisition name
     * @param properties the full set of properties to write
     *
     * @throws IOException if the properties file can not be written
     */
    public void write(final String name, final Map<String, String> properties) throws IOException {
        final Path directory = this.requisitionPath(name);
        Files.createDirectories(directory);

        final Path temp = Files.createTempFile(directory, "." + PROPERTIES_FILE, ".tmp");
        try {
            Files.writeString(temp, serialize(properties), StandardCharsets.ISO_8859_1);
            Files.move(temp, directory.resolve(PROPERTIES_FILE),
                       StandardCopyOption.ATOMIC_MOVE,
                       StandardCopyOption.REPLACE_EXISTING);

        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * Serializes properties with {@link Properties#store} so escaping stays
     * symmetric with the {@link Properties#load} based readers - backslashes,
     * newlines and non-Latin-1 characters survive the round-trip. The store
     * output is re-ordered to sorted keys and stripped of the timestamp
     * comment to keep the files deterministic and diff-friendly.
     */
    private static String serialize(final Map<String, String> properties) throws IOException {
        final Properties store = new Properties();
        store.putAll(properties);

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        store.store(buffer, null);

        final List<String> lines = new ArrayList<>();
        for (final String line : buffer.toString(StandardCharsets.ISO_8859_1).split("\\R")) {
            if (!line.startsWith("#")) {
                lines.add(line);
            }
        }

        lines.sort(Comparator.naturalOrder());
        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }

    /**
     * Deletes a requisition configuration including its folder.
     *
     * @param name the requisition name
     *
     * @return {@code true} if the requisition existed and was deleted
     *
     * @throws IOException if the folder can not be deleted
     */
    public boolean delete(final String name) throws IOException {
        final Path directory = this.requisitionPath(name);
        if (!Files.isDirectory(directory) || !Files.exists(directory.resolve(PROPERTIES_FILE))) {
            return false;
        }

        try (final Stream<Path> paths = Files.walk(directory)) {
            final List<Path> toDelete = paths.sorted(Comparator.reverseOrder()).toList();
            for (final Path path : toDelete) {
                Files.delete(path);
            }
        }

        return true;
    }

    private Path requisitionPath(final String name) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid requisition name: " + name);
        }

        return this.requisitionsPath.resolve(name);
    }

    private Path propertiesPath(final String name) {
        return this.requisitionPath(name).resolve(PROPERTIES_FILE);
    }
}
