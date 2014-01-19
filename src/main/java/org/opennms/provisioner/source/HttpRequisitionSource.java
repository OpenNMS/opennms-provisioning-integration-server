package org.opennms.provisioner.source;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.configuration.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequisitionSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequisitionSource.class);

    private final String instance;
    private final Configuration config;

    private HttpRequisitionSource(final String instance, final Configuration config) {
        this.instance = instance;
        this.config = config;
    }

    @Override
    public Object dump() throws Exception {
        LOGGER.info("HttpRequisitionSource at work....");
        Requisition requisition = null;

        if (getUrl() != null) {
            HttpClientBuilder builder = HttpClientBuilder.create();

            // If username and password was found, inject the credentials
            if (getUserName() != null && getPassword() != null) {

                CredentialsProvider provider = new BasicCredentialsProvider();

                // Create the authentication scope
                AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM);

                // Create credential pair
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(getUserName(), getPassword());

                // Inject the credentials
                provider.setCredentials(scope, credentials);

                // Set the default credentials provider
                builder.setDefaultCredentialsProvider(provider);
            }

            HttpClient client = builder.build();
            HttpGet request = new HttpGet(getUrl());
            HttpResponse response = client.execute(request);
            try {

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                JAXBContext jaxbContext = JAXBContext.newInstance(Requisition.class);

                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                requisition = (Requisition) jaxbUnmarshaller.unmarshal(bufferedReader);

            } catch (JAXBException e) {
                LOGGER.error("The responce did not contain a valid requisition as xml.", e);
            }
            LOGGER.debug("Got Requisition {}", requisition);
        } else {
            LOGGER.error("Parameter requisition.url is missing in requisition.properties");
        }
        return requisition;
    }

    public final String getUserName() {
        return this.config.getString("requisition.username");
    }

    public final String getPassword() {
        return this.config.getString("requisition.password");
    }

    public final String getUrl() {
        return this.config.getString("requisition.url");
    }

    public static class Factory implements Source.Factory {

        @Override
        public Source create(final String instance, final Configuration config) {
            return new HttpRequisitionSource(instance, config);
        }
    }
}
