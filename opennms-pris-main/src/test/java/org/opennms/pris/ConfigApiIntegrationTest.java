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

package org.opennms.pris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opennms.pris.config.GlobalApacheConfiguration;
import org.opennms.pris.driver.HttpServerDriver;

/**
 * Full-stack tests of the configuration REST API: an embedded server on an
 * ephemeral port, exercised over HTTP, down to the property files and back up
 * through the requisition endpoint.
 */
public class ConfigApiIntegrationTest {

    private static final String TOKEN = "integration-test-token";

    private static final String REQUISITION_XML =
            "<model-import xmlns=\"http://xmlns.opennms.org/xsd/config/model-import\" foreign-source=\"%s\">"
            + "<node node-label=\"%s\" foreign-id=\"%s\"/>"
            + "</model-import>";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    private Server server;
    private int port;

    @After
    public void stopServer() throws Exception {
        if (this.server != null) {
            this.server.stop();
            this.server = null;
        }
    }

    private Path createBase(final String... extraGlobalProperties) throws IOException {
        final Path base = this.folder.newFolder().toPath();
        Files.createDirectory(base.resolve("requisitions"));

        final StringBuilder global = new StringBuilder()
                .append("driver = http\n")
                .append("host = 127.0.0.1\n")
                .append("port = 0\n");
        for (final String line : extraGlobalProperties) {
            global.append(line).append('\n');
        }
        Files.writeString(base.resolve("global.properties"), global.toString());

        return base;
    }

    private void startServer(final Path base) throws Exception {
        // The requisition endpoint resolves its configuration through the
        // Starter, which reads the pris.config system property
        System.setProperty("pris.config", base.toString());
        Starter.resetConfigManager();

        final HttpServerDriver driver = (HttpServerDriver)
                new HttpServerDriver.Factory().create(new GlobalApacheConfiguration(base));

        this.server = driver.start();
        this.port = ((ServerConnector) this.server.getConnectors()[0]).getLocalPort();
    }

    private HttpRequest.Builder request(final String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + this.port + path));
    }

    private HttpRequest.Builder authorized(final String path) {
        return this.request(path).header("Authorization", "Bearer " + TOKEN);
    }

    private HttpResponse<String> send(final HttpRequest request) throws Exception {
        return this.client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void apiIsAbsentWhenNotEnabled() throws Exception {
        this.startServer(this.createBase());

        final HttpResponse<String> response = this.send(this.authorized("/api/v1/config/global").build());
        assertEquals(404, response.statusCode());
    }

    @Test
    public void enablingWithoutTokenRefusesToStart() throws Exception {
        final Path base = this.createBase("config.api.enabled = true");

        final HttpServerDriver driver = (HttpServerDriver)
                new HttpServerDriver.Factory().create(new GlobalApacheConfiguration(base));

        assertThrows(IllegalStateException.class, driver::start);
    }

    @Test
    public void requestsWithoutValidTokenAreRejected() throws Exception {
        this.startServer(this.createBase("config.api.enabled = true",
                                         "config.api.token = " + TOKEN));

        assertEquals(401, this.send(this.request("/api/v1/config/global").build()).statusCode());
        assertEquals(401, this.send(this.request("/api/v1/config/global")
                .header("Authorization", "Bearer wrong-token").build()).statusCode());

        assertEquals(200, this.send(this.authorized("/api/v1/config/global").build()).statusCode());
    }

    @Test
    public void metadataListsSourcesWithParameters() throws Exception {
        this.startServer(this.createBase("config.api.enabled = true",
                                         "config.api.token = " + TOKEN));

        final HttpResponse<String> response = this.send(this.authorized("/api/v1/config/metadata").build());
        assertEquals(200, response.statusCode());

        final JsonNode metadata = this.json.readTree(response.body());

        JsonNode fileSource = null;
        JsonNode echoMapper = null;
        for (final JsonNode source : metadata.get("sources")) {
            if ("file".equals(source.get("name").asText())) {
                fileSource = source;
            }
        }
        for (final JsonNode mapper : metadata.get("mappers")) {
            if ("echo".equals(mapper.get("name").asText())) {
                echoMapper = mapper;
            }
        }

        assertTrue("file source missing from metadata", fileSource != null);
        assertTrue("echo mapper missing from metadata", echoMapper != null);

        // The file source describes its single required parameter
        final JsonNode parameter = fileSource.get("parameters").get(0);
        assertEquals("file", parameter.get("name").asText());
        assertTrue(parameter.get("required").asBoolean());
    }

    @Test
    public void validateReportsResultAndMatchCount() throws Exception {
        final Path base = this.createBase("config.api.enabled = true",
                                          "config.api.token = " + TOKEN);
        this.startServer(base);

        final Path requisitionFile = base.resolve("valid.xml");
        Files.writeString(requisitionFile, REQUISITION_XML.formatted("checkMe", "node-a", "node-a"));

        // A candidate that has never been saved validates - and stays unsaved
        final String candidate = this.json.writeValueAsString(java.util.Map.of(
                "source", "file",
                "source.file", requisitionFile.toAbsolutePath().toString(),
                "mapper", "echo"));

        final HttpResponse<String> ok = this.send(this.authorized("/api/v1/config/requisitions/checkMe/validate")
                .POST(HttpRequest.BodyPublishers.ofString(candidate)).build());
        assertEquals(200, ok.statusCode());
        final JsonNode okResult = this.json.readTree(ok.body());
        assertTrue(okResult.get("ok").asBoolean());
        assertEquals(1, okResult.get("nodes").asInt());
        assertFalse(Files.exists(base.resolve("requisitions").resolve("checkMe")));

        // A missing referenced file is reported, not saved
        final String broken = this.json.writeValueAsString(java.util.Map.of(
                "source", "file",
                "source.file", base.resolve("does-not-exist.xml").toString(),
                "mapper", "echo"));

        final HttpResponse<String> bad = this.send(this.authorized("/api/v1/config/requisitions/checkMe/validate")
                .POST(HttpRequest.BodyPublishers.ofString(broken)).build());
        assertEquals(200, bad.statusCode());
        assertFalse(this.json.readTree(bad.body()).get("ok").asBoolean());

        // A missing required parameter is a validation error
        final HttpResponse<String> incomplete = this.send(this.authorized("/api/v1/config/requisitions/checkMe/validate")
                .POST(HttpRequest.BodyPublishers.ofString("{\"source\": \"file\", \"mapper\": \"echo\"}")).build());
        assertEquals(400, incomplete.statusCode());
        assertThat(incomplete.body(), containsString("source.file"));

        // With an empty body the saved configuration is validated
        this.send(this.authorized("/api/v1/config/requisitions/checkMe")
                .PUT(HttpRequest.BodyPublishers.ofString(candidate)).build());
        final HttpResponse<String> saved = this.send(this.authorized("/api/v1/config/requisitions/checkMe/validate")
                .POST(HttpRequest.BodyPublishers.noBody()).build());
        assertEquals(200, saved.statusCode());
        assertTrue(this.json.readTree(saved.body()).get("ok").asBoolean());
    }

    @Test
    public void crudRoundTripReachesTheRequisitionEndpoint() throws Exception {
        final Path base = this.createBase("config.api.enabled = true",
                                          "config.api.token = " + TOKEN);
        this.startServer(base);

        // A requisition XML file for the 'file' source to serve
        final Path requisitionFile = base.resolve("myTest.xml");
        Files.writeString(requisitionFile, REQUISITION_XML.formatted("myTest", "node-a", "node-a"));

        // Create through the API
        final String body = this.json.writeValueAsString(java.util.Map.of(
                "source", "file",
                "source.file", requisitionFile.toAbsolutePath().toString(),
                "mapper", "echo"));

        final HttpResponse<String> created = this.send(this.authorized("/api/v1/config/requisitions/myTest")
                .PUT(HttpRequest.BodyPublishers.ofString(body)).build());
        assertEquals(201, created.statusCode());
        assertEquals("file", this.json.readTree(created.body()).get("properties").get("source").asText());

        // Visible in the listing
        final HttpResponse<String> list = this.send(this.authorized("/api/v1/config/requisitions").build());
        assertThat(list.body(), containsString("\"myTest\""));

        // Served by the existing requisition endpoint without a restart
        final HttpResponse<String> requisition = this.send(this.request("/requisitions/myTest").build());
        assertEquals(200, requisition.statusCode());
        assertThat(requisition.body(), containsString("node-label=\"node-a\""));

        // The preview endpoint generates the same requisition
        final HttpResponse<String> preview = this.send(this.authorized("/api/v1/config/requisitions/myTest/preview")
                .POST(HttpRequest.BodyPublishers.noBody()).build());
        assertEquals(200, preview.statusCode());
        assertThat(preview.body(), containsString("node-label=\"node-a\""));

        // A hand edit on disk shows up through the API and the requisition
        final Path otherFile = base.resolve("other.xml");
        Files.writeString(otherFile, REQUISITION_XML.formatted("myTest", "node-b", "node-b"));
        Files.writeString(base.resolve("requisitions").resolve("myTest").resolve("requisition.properties"),
                          "source = file\nsource.file = " + otherFile.toAbsolutePath() + "\nmapper = echo\n");

        final HttpResponse<String> afterEdit = this.send(this.authorized("/api/v1/config/requisitions/myTest").build());
        assertThat(afterEdit.body(), containsString("other.xml"));
        assertThat(this.send(this.request("/requisitions/myTest").build()).body(),
                   containsString("node-label=\"node-b\""));

        // Delete through the API
        assertEquals(204, this.send(this.authorized("/api/v1/config/requisitions/myTest")
                .DELETE().build()).statusCode());
        assertEquals(404, this.send(this.authorized("/api/v1/config/requisitions/myTest").build()).statusCode());
        assertFalse(Files.exists(base.resolve("requisitions").resolve("myTest")));
    }

    @Test
    public void validationRejectsBrokenConfigurations() throws Exception {
        final Path base = this.createBase("config.api.enabled = true",
                                          "config.api.token = " + TOKEN);
        this.startServer(base);

        // Unknown source
        final HttpResponse<String> unknownSource = this.send(this.authorized("/api/v1/config/requisitions/broken")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"source\": \"no-such-source\"}")).build());
        assertEquals(400, unknownSource.statusCode());
        assertThat(unknownSource.body(), containsString("Unknown source"));

        // Missing source
        assertEquals(400, this.send(this.authorized("/api/v1/config/requisitions/broken")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"mapper\": \"echo\"}")).build()).statusCode());

        // Source without mapper and without a default mapper
        final HttpResponse<String> noMapper = this.send(this.authorized("/api/v1/config/requisitions/broken")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"source\": \"file\"}")).build());
        assertEquals(400, noMapper.statusCode());
        assertThat(noMapper.body(), containsString("mapper"));

        // Not JSON at all
        assertEquals(400, this.send(this.authorized("/api/v1/config/requisitions/broken")
                .PUT(HttpRequest.BodyPublishers.ofString("not json")).build()).statusCode());

        // Nothing was written for any of the attempts
        assertFalse(Files.exists(base.resolve("requisitions").resolve("broken")));
    }

    @Test
    public void traversalAttemptsWriteNothing() throws Exception {
        final Path base = this.createBase("config.api.enabled = true",
                                          "config.api.token = " + TOKEN);
        this.startServer(base);

        final HttpResponse<String> response = this.send(this.authorized("/api/v1/config/requisitions/..%2Fevil")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"source\": \"file\", \"mapper\": \"echo\"}")).build());

        assertTrue("expected a client error, got " + response.statusCode(),
                   response.statusCode() >= 400);
        assertFalse(Files.exists(base.resolve("evil")));
        assertFalse(Files.exists(base.resolve("requisitions").resolve("evil")));
    }
}
