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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.opennms.pris.RequisitionGenerator;
import org.opennms.pris.RequisitionXml;
import org.opennms.pris.Starter;
import org.opennms.pris.api.InstanceConfiguration;
import org.opennms.pris.api.ParameterDescriptor;
import org.opennms.pris.model.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration REST API published at {@code /api/v1/config}.
 *
 * The API is a thin CRUD layer over the property files an operator edits by
 * hand - see {@link RequisitionConfigRepository}. Every request must carry the
 * configured bearer token.
 */
public class ConfigApiHandler extends Handler.Abstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigApiHandler.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper json = new ObjectMapper();

    // The token every request has to present
    private final String token;

    // The global.properties file, exposed read-only
    private final Path globalPropertiesPath;

    private final RequisitionConfigRepository repository;

    public ConfigApiHandler(final String token,
                            final Path globalPropertiesPath,
                            final RequisitionConfigRepository repository) {
        this.token = token;
        this.globalPropertiesPath = globalPropertiesPath;
        this.repository = repository;
    }

    @Override
    public boolean handle(final Request request,
                          final Response response,
                          final Callback callback) throws Exception {
        try {
            if (!this.isAuthorized(request)) {
                this.writeError(response, callback, 401, "Missing or invalid bearer token");
                return true;
            }

            final String path = Request.getPathInContext(request);
            final String method = request.getMethod();

            if ("/global".equals(path) && "GET".equals(method)) {
                this.handleGetGlobal(response, callback);

            } else if ("/metadata".equals(path) && "GET".equals(method)) {
                this.handleGetMetadata(response, callback);

            } else if ("/requisitions".equals(path) && "GET".equals(method)) {
                this.handleListRequisitions(response, callback);

            } else if (path.startsWith("/requisitions/")) {
                this.handleRequisition(request, response, callback, path, method);

            } else {
                this.writeError(response, callback, 404, "No such resource: " + path);
            }

        } catch (final Exception ex) {
            LOGGER.error("Config API request failed", ex);
            this.writeError(response, callback, 500, "Internal error: " + ex.getMessage());
        }

        return true;
    }

    private void handleRequisition(final Request request,
                                   final Response response,
                                   final Callback callback,
                                   final String path,
                                   final String method) throws Exception {
        final String[] parts = path.substring("/requisitions/".length()).split("/", 2);
        final String name = parts[0];
        final String action = parts.length > 1 ? parts[1] : null;

        if (!RequisitionConfigRepository.isValidName(name)) {
            this.writeError(response, callback, 400, "Invalid requisition name: " + name);
            return;
        }

        if (action == null && "GET".equals(method)) {
            this.handleGetRequisition(response, callback, name);

        } else if (action == null && "PUT".equals(method)) {
            this.handlePutRequisition(request, response, callback, name);

        } else if (action == null && "DELETE".equals(method)) {
            this.handleDeleteRequisition(response, callback, name);

        } else if ("preview".equals(action) && "POST".equals(method)) {
            this.handlePreviewRequisition(response, callback, name);

        } else if ("validate".equals(action) && "POST".equals(method)) {
            this.handleValidateRequisition(request, response, callback, name);

        } else {
            this.writeError(response, callback, 404, "No such resource: " + path);
        }
    }

    private void handleGetGlobal(final Response response,
                                 final Callback callback) throws Exception {
        final ObjectNode result = this.json.createObjectNode();

        if (Files.exists(this.globalPropertiesPath)) {
            final Properties properties = new Properties();
            try (final InputStream in = Files.newInputStream(this.globalPropertiesPath)) {
                properties.load(in);
            }

            for (final String key : properties.stringPropertyNames().stream().sorted().toList()) {
                result.put(key, properties.getProperty(key));
            }
        }

        this.writeJson(response, callback, 200, result);
    }

    private void handleGetMetadata(final Response response,
                                   final Callback callback) throws Exception {
        final ObjectNode result = this.json.createObjectNode();

        final ArrayNode sources = result.putArray("sources");
        RequisitionGenerator.getSourceIdentifiers().stream().sorted().forEach(identifier ->
                this.describeImplementation(sources.addObject(), identifier,
                        RequisitionGenerator.getSourceFactory(identifier).getParameters()));

        final ArrayNode mappers = result.putArray("mappers");
        RequisitionGenerator.getMapperIdentifiers().stream().sorted().forEach(identifier ->
                this.describeImplementation(mappers.addObject(), identifier,
                        RequisitionGenerator.getMapperFactory(identifier).getParameters()));

        this.writeJson(response, callback, 200, result);
    }

    private void describeImplementation(final ObjectNode node,
                                        final String identifier,
                                        final List<ParameterDescriptor> parameters) {
        node.put("name", identifier);

        final ArrayNode parameterList = node.putArray("parameters");
        for (final ParameterDescriptor parameter : parameters) {
            final ObjectNode entry = parameterList.addObject();
            entry.put("name", parameter.getName());
            entry.put("description", parameter.getDescription());
            entry.put("required", parameter.isRequired());
            entry.put("secret", parameter.isSecret());
        }
    }

    private void handleListRequisitions(final Response response,
                                        final Callback callback) throws Exception {
        final ArrayNode result = this.json.createArrayNode();

        for (final String name : this.repository.list()) {
            final ObjectNode entry = result.addObject();
            entry.put("name", name);

            try {
                final Map<String, String> properties = this.repository.read(name);
                entry.put("source", properties.get("source"));
                entry.put("mapper", properties.get("mapper"));

            } catch (final NoSuchFileException ex) {
                // Deleted between list and read - skip the details
            }
        }

        this.writeJson(response, callback, 200, result);
    }

    private void handleGetRequisition(final Response response,
                                      final Callback callback,
                                      final String name) throws Exception {
        try {
            this.writeJson(response, callback, 200, this.toRequisitionNode(name));

        } catch (final NoSuchFileException ex) {
            this.writeError(response, callback, 404, "No such requisition: " + name);
        }
    }

    private void handlePutRequisition(final Request request,
                                      final Response response,
                                      final Callback callback,
                                      final String name) throws Exception {
        final Map<String, String> properties;
        try {
            properties = this.parseProperties(readBody(request));

        } catch (final BadRequestException ex) {
            this.writeError(response, callback, 400, ex.getMessage());
            return;
        }

        final ObjectNode errors = this.validateProperties(name, properties);
        if (!errors.isEmpty()) {
            this.writeValidationErrors(response, callback, errors);
            return;
        }

        final boolean created = !this.repository.exists(name);
        this.repository.write(name, properties);

        this.writeJson(response, callback, created ? 201 : 200, toRequisitionNode(name, properties));
    }

    private void handleDeleteRequisition(final Response response,
                                         final Callback callback,
                                         final String name) throws Exception {
        if (this.repository.delete(name)) {
            response.setStatus(204);
            callback.succeeded();

        } else {
            this.writeError(response, callback, 404, "No such requisition: " + name);
        }
    }

    private void handlePreviewRequisition(final Response response,
                                          final Callback callback,
                                          final String name) throws Exception {
        if (!this.repository.exists(name)) {
            this.writeError(response, callback, 404, "No such requisition: " + name);
            return;
        }

        try {
            final Requisition requisition = new RequisitionGenerator(name).generate(name);

            // Marshal into memory first so a failure can still produce an
            // error response instead of a truncated, already-committed 200
            final ByteArrayOutputStream xml = new ByteArrayOutputStream();
            RequisitionXml.marshal(requisition, xml);

            response.setStatus(200);
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/xml");
            try (final OutputStream out = Content.Sink.asOutputStream(response)) {
                xml.writeTo(out);
            }
            callback.succeeded();

        } catch (final Exception ex) {
            LOGGER.warn("Preview of requisition '{}' failed", name, ex);
            this.writeError(response, callback, 422, "Requisition generation failed: " + ex.getMessage());
        }
    }

    /**
     * Dry-runs a requisition configuration and reports whether generation
     * succeeds and how many nodes it would deliver.
     *
     * With a JSON body the given candidate properties are validated and run
     * without anything being written to disk - the way a UI checks a
     * configuration (including the existence and parseability of referenced
     * files) before saving it. With an empty body the saved configuration of
     * the requisition is run instead.
     */
    private void handleValidateRequisition(final Request request,
                                           final Response response,
                                           final Callback callback,
                                           final String name) throws Exception {
        final String body = readBody(request);

        Map<String, String> properties;
        if (body.isBlank()) {
            try {
                properties = this.repository.read(name);

            } catch (final NoSuchFileException ex) {
                this.writeError(response, callback, 404, "No such requisition: " + name);
                return;
            }

        } else {
            try {
                properties = this.parseProperties(body);

            } catch (final BadRequestException ex) {
                this.writeError(response, callback, 400, ex.getMessage());
                return;
            }
        }

        final ObjectNode errors = this.validateProperties(name, properties);
        if (!errors.isEmpty()) {
            this.writeValidationErrors(response, callback, errors);
            return;
        }

        final ObjectNode result = this.json.createObjectNode();
        try {
            final Requisition requisition = new RequisitionGenerator(name,
                    Starter.getConfigManager().getCandidateConfigWithGlobals(name, properties))
                    .generate(name);

            if (requisition == null || requisition.getNodes() == null) {
                result.put("ok", false);
                result.put("message", "The source delivered no result - check the source configuration");

            } else {
                result.put("ok", true);
                result.put("nodes", requisition.getNodes().size());
                result.put("interfaces", requisition.getNodes().stream()
                        .mapToInt(node -> node.getInterfaces() == null ? 0 : node.getInterfaces().size())
                        .sum());
            }

        } catch (final Exception ex) {
            LOGGER.info("Validation of requisition '{}' failed", name, ex);
            result.put("ok", false);
            result.put("message", rootMessage(ex));
        }

        this.writeJson(response, callback, 200, result);
    }

    /**
     * Digs out the most specific message of an exception chain.
     */
    private static String rootMessage(final Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        final String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    /**
     * Reads the full request body as text.
     *
     * Reading the stream (instead of trusting Content-Length) also covers
     * chunked requests, which report no length.
     */
    private static String readBody(final Request request) throws Exception {
        try (final InputStream in = Content.Source.asInputStream(request)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Parses a request body as a flat JSON object of property values.
     * JSON null values are treated as absent keys.
     */
    private Map<String, String> parseProperties(final String body) throws BadRequestException {
        final JsonNode tree;
        try {
            tree = this.json.readTree(body);

        } catch (final IOException ex) {
            throw new BadRequestException("Request body is not valid JSON");
        }

        if (tree == null || !tree.isObject()) {
            throw new BadRequestException("Request body must be a JSON object of properties");
        }

        final Map<String, String> properties = new TreeMap<>();
        final var fields = tree.fields();
        while (fields.hasNext()) {
            final var field = fields.next();
            if (field.getValue().isNull()) {
                continue;
            }
            if (!field.getValue().isValueNode()) {
                throw new BadRequestException("Property '" + field.getKey() + "' must be a scalar value");
            }

            properties.put(field.getKey(), field.getValue().asText());
        }

        return properties;
    }

    /**
     * Signals a request the client has to fix, carrying the message to return.
     */
    private static class BadRequestException extends Exception {

        BadRequestException(final String message) {
            super(message);
        }
    }

    /**
     * Validates a requisition configuration against the source and mapper
     * implementations known to this instance.
     *
     * Validation runs against the same merged view generation uses - the
     * candidate properties with global.properties filled in - so a key
     * supplied globally satisfies a requirement here exactly like at runtime.
     * A required key only has to be present; an explicitly empty value is
     * allowed (some parameters, like the OCS mapper's map files, are
     * present-but-empty by convention).
     */
    private ObjectNode validateProperties(final String name, final Map<String, String> properties) {
        final ObjectNode errors = this.json.createObjectNode();
        final InstanceConfiguration merged =
                Starter.getConfigManager().getCandidateConfigWithGlobals(name, properties);

        final String source = merged.getString("source", null);
        if (source == null || source.isBlank()) {
            errors.put("source", "Property 'source' is required");

        } else if (!RequisitionGenerator.getSourceIdentifiers().contains(source)) {
            errors.put("source", "Unknown source: " + source);

        } else {
            requireParameters(errors, merged, "source", source,
                    RequisitionGenerator.getSourceFactory(source).getParameters());
        }

        final String mapper = merged.getString("mapper", null);
        if (mapper != null && !mapper.isBlank()) {
            if (!RequisitionGenerator.getMapperIdentifiers().contains(mapper)) {
                errors.put("mapper", "Unknown mapper: " + mapper);

            } else {
                requireParameters(errors, merged, "mapper", mapper,
                        RequisitionGenerator.getMapperFactory(mapper).getParameters());
            }

        } else if (source != null && !source.isBlank()
                && !RequisitionGenerator.getMapperIdentifiers().contains("default." + source)) {
            errors.put("mapper", "Property 'mapper' is required: source '" + source + "' has no default mapper");
        }

        return errors;
    }

    private static void requireParameters(final ObjectNode errors,
                                          final InstanceConfiguration merged,
                                          final String prefix,
                                          final String implementation,
                                          final List<ParameterDescriptor> parameters) {
        for (final ParameterDescriptor parameter : parameters) {
            if (!parameter.isRequired()) {
                continue;
            }

            final String key = prefix + "." + parameter.getName();
            if (!merged.containsKey(key)) {
                errors.put(key, "Property '" + key + "' is required for " + prefix + " '" + implementation + "'");
            }
        }
    }

    private void writeValidationErrors(final Response response,
                                       final Callback callback,
                                       final ObjectNode errors) throws Exception {
        final ObjectNode body = this.json.createObjectNode();
        body.put("status", 400);
        body.put("message", "Validation failed");
        body.set("errors", errors);

        this.writeJson(response, callback, 400, body);
    }

    private ObjectNode toRequisitionNode(final String name) throws Exception {
        return toRequisitionNode(name, this.repository.read(name));
    }

    private ObjectNode toRequisitionNode(final String name, final Map<String, String> propertyMap) {
        final ObjectNode result = this.json.createObjectNode();
        result.put("name", name);

        final ObjectNode properties = result.putObject("properties");
        for (final Map.Entry<String, String> entry : new TreeMap<>(propertyMap).entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private boolean isAuthorized(final Request request) {
        final String header = request.getHeaders().get(HttpHeader.AUTHORIZATION);
        // The auth scheme is case-insensitive per RFC 9110
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return false;
        }

        final String presented = header.substring(BEARER_PREFIX.length());
        return MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8),
                                     this.token.getBytes(StandardCharsets.UTF_8));
    }

    private void writeError(final Response response,
                            final Callback callback,
                            final int status,
                            final String message) throws Exception {
        final ObjectNode body = this.json.createObjectNode();
        body.put("status", status);
        body.put("message", message);

        this.writeJson(response, callback, status, body);
    }

    private void writeJson(final Response response,
                           final Callback callback,
                           final int status,
                           final JsonNode body) throws Exception {
        response.setStatus(status);
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json");

        try (final OutputStream out = Content.Sink.asOutputStream(response)) {
            this.json.writeValue(out, body);
        }

        callback.succeeded();
    }
}
