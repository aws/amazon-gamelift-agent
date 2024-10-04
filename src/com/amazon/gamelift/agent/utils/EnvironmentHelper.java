/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.utils;

import com.amazon.gamelift.agent.model.constants.EnvironmentConstants;
import com.amazonaws.AmazonClientException;
import com.amazonaws.util.EC2MetadataUtils;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
public class EnvironmentHelper {
    private static Map<String, String> getHostInfoFromEC2Metadata() throws AmazonClientException {
        return ImmutableMap.of(
                EnvironmentConstants.HOST_ID_KEY, EC2MetadataUtils.getInstanceId(),
                // Network metadata requires a mac address to use helper methods. Using getData for default mac resolution
                EnvironmentConstants.HOST_NAME_KEY, EC2MetadataUtils.getData(EnvironmentConstants.INSTANCE_HOSTNAME_KEY),
                EnvironmentConstants.HOST_PUBLICIPV4_KEY, EC2MetadataUtils.getData(EnvironmentConstants.INSTANCE_PUBLICIPV4_KEY),
                EnvironmentConstants.HOST_INSTANCE_TYPE_KEY, EC2MetadataUtils.getInstanceType(),
                EnvironmentConstants.AMI_ID, EC2MetadataUtils.getAmiId());
    }

    /**
     * Utility method to log the current EC2Metadata
     */
    public static void logEC2Metadata() {
        try {
	    final Map<String, String> hostInfo;

            if (System.getenv("ECS_CONTAINER_METADATA_URI_V4") != null) {
                hostInfo = getHostInfoFromECSMetadata();
            } else {
                hostInfo = getHostInfoFromEC2Metadata();
            }

            log.info("EC2 metadata: "
                            + "instanceId: {}, "
                            + "instanceType: {}, "
                            + "PublicIpAddress: {}, "
                            + "DnsName (hostName): {}, "
                            + "AMI ID: {}",
                    hostInfo.get(EnvironmentConstants.HOST_ID_KEY),
                    hostInfo.get(EnvironmentConstants.HOST_INSTANCE_TYPE_KEY),
                    hostInfo.get(EnvironmentConstants.HOST_PUBLICIPV4_KEY),
                    hostInfo.get(EnvironmentConstants.HOST_NAME_KEY),
                    hostInfo.get(EnvironmentConstants.AMI_ID)
            );
        } catch (final Exception e) {
            log.warn("EC2 metadata could not be logged. ", e);
        }
    }

    /**
     * Utility method to log the current ECSMetadata
     */
    private static Map<String, String> getHostInfoFromECSMetadata() throws Exception {
        String metadataEndpoint = System.getenv("ECS_CONTAINER_METADATA_URI_V4");
        if (metadataEndpoint == null) {
            throw new RuntimeException("ECS metadata endpoint not available");
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(metadataEndpoint + "/task"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to retrieve ECS metadata: HTTP " + response.statusCode());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode taskMetadata = mapper.readTree(response.body());

        Map<String, String> hostInfo = new HashMap<>();
        hostInfo.put(EnvironmentConstants.HOST_ID_KEY, taskMetadata.path("TaskARN").asText());
        hostInfo.put(EnvironmentConstants.HOST_NAME_KEY, taskMetadata.path("Containers").get(0).path("Name").asText());
        hostInfo.put(EnvironmentConstants.HOST_PUBLICIPV4_KEY, getPublicIpv4(taskMetadata));
        hostInfo.put(EnvironmentConstants.HOST_INSTANCE_TYPE_KEY, "ECS_CONTAINER");
        hostInfo.put(EnvironmentConstants.AMI_ID, taskMetadata.path("Containers").get(0).path("Image").asText());

        return hostInfo;
    }

    private static String getPublicIpv4(JsonNode taskMetadata) {
        List<String> privateIpPrefixes = Arrays.asList("10.", "172.16.", "172.17.", "172.18.", "172.19.", "172.20.",
                                                   "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.",
                                                   "172.27.", "172.28.", "172.29.", "172.30.", "172.31.", "192.168.");

        JsonNode networks = taskMetadata.path("Containers").get(0).path("Networks");
        for (JsonNode network : networks) {
            JsonNode ipv4Addresses = network.path("IPv4Addresses");
            for (JsonNode ipv4 : ipv4Addresses) {
                String ip = ipv4.asText();
                if (!isPrivateIp(ip, privateIpPrefixes)) {
                    return ip;
                }
            }
        }
        return "";
    }

    private static boolean isPrivateIp(String ip, List<String> privateIpPrefixes) {
        for (String prefix : privateIpPrefixes) {
	   if (ip.startsWith(prefix)) {
	   	return true;
	   }
	}
	return false;
    }
}
