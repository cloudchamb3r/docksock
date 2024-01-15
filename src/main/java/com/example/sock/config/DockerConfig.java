package com.example.sock.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerConfig {
    @Bean
    DockerClientConfig dockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://127.0.0.1:2375")
                .withDockerTlsVerify(false)
                .build();
    }

    @Bean
    DockerHttpClient dockerHttpClient(DockerClientConfig clientConfig) {
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .build();
    }

    @Bean
    DockerClient dockerClient(DockerClientConfig clientConfig, DockerHttpClient httpClient) {
        return DockerClientImpl.getInstance(clientConfig, httpClient);
    }
}
