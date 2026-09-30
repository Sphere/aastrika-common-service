package org.aastrika.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OpenSearchConfig {


    @Bean(name = "openSearchClient", destroyMethod = "close")
    public RestHighLevelClient getSbOsRestClient(
            @Value("${sb.lern.es.host.list}") String[] hostList,
            @Value("${sb.lern.es.username:}") String user,
            @Value("${sb.lern.es.password:}") String password) {
        return createRestClient(hostList, user, password);
    }

    private RestHighLevelClient createRestClient(String[] hosts, String user, String password) {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        HttpHost[] httpHosts = new HttpHost[hosts.length];
        for (int i = 0; i < httpHosts.length; i++) {
            String hostIp = hosts[i].split(":")[0];
            String hostPort = hosts[i].split(":")[1];
            httpHosts[i] = new HttpHost(hostIp, Integer.parseInt(hostPort));
        }
        RestClientBuilder builder = RestClient.builder(httpHosts).setHttpClientConfigCallback(
                httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        return new RestHighLevelClient(builder);
    }
}