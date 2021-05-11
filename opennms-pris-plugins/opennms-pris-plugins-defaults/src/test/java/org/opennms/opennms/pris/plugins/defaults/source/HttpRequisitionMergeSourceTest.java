package org.opennms.opennms.pris.plugins.defaults.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.opennms.pris.api.MockInstanceConfiguration;
import org.opennms.pris.model.MetaData;
import org.opennms.pris.model.Requisition;
import org.opennms.pris.model.RequisitionNode;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpRequisitionMergeSourceTest {

    @Before
    public void setup() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/source1.xml", new TestHandler(Paths.get("src/test/resources/source1.xml")));
        server.createContext("/source2.xml", new TestHandler(Paths.get("src/test/resources/source2.xml")));
        server.setExecutor(null);
        server.start();
    }

    @Test
    public void testMetaDataMerging() throws Exception {
        final MockInstanceConfiguration config = new MockInstanceConfiguration("test");
        config.set("encoding", "ISO-8859-1");
        config.set("A.url", "http://localhost:8000/source1.xml");
        config.set("B.url", "http://localhost:8000/source2.xml");
        final HttpRequisitionMergeSource httpRequisitionMergeSource = new HttpRequisitionMergeSource(config);

        final Requisition result = (Requisition) httpRequisitionMergeSource.dump();
        assertEquals(1, result.getNodes().size());
        final RequisitionNode requisitionNode = result.getNodes().get(0);
        assertEquals(2, requisitionNode.getMetaDatas().size());
        assertTrue(requisitionNode.getMetaDatas().stream().anyMatch(e -> e.equals(new MetaData("requisition", "env", "pro"))));
        assertTrue(requisitionNode.getMetaDatas().stream().anyMatch(e -> e.equals(new MetaData("requisition", "app", "application"))));
    }

    static class TestHandler implements HttpHandler {
        final byte[] contents;

        public TestHandler(final Path path) throws Exception {
            this.contents = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).getBytes();
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            final Headers headers = httpExchange.getResponseHeaders();
            headers.add("Content-Type", "application/xml");
            httpExchange.sendResponseHeaders(200, contents.length);
            final OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(contents, 0, contents.length);
            outputStream.close();
        }
    }
}
