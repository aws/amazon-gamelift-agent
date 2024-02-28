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
            Map<String, String> hostInfo = getHostInfoFromEC2Metadata();
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
        } catch (Exception e) {
            log.warn("EC2 metadata could not be logged. ", e);
        }
    }
}
