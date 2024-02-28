/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.gamelift.agent;

import static com.amazon.gamelift.agent.Application.JAVA_PROP_FORKJOINPOOL_PARALLELISM_KEY;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_FORKJOINPOOL_PARALLELISM_VALUE;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_NEG_TTL_KEY;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_NEG_TTL_VALUE;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_NETWORK_ADDR_CACHE_TTL_KEY;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_NETWORK_ADDR_CACHE_TTL_VALUE;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_PREFER_IPV4_STACK_KEY;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_PREFER_IPV4_STACK_VALUE;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_SECURITY_EGD_KEY;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_SECURITY_EGD_VALUE;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_TTL_KEY;
import static com.amazon.gamelift.agent.Application.JAVA_PROP_TTL_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class ApplicationTest {

    @Test
    @SneakyThrows
    public void GIVEN_systemProperties_WHEN_mainIsCalled_THEN_javaSystemPropertiesAreSet() {
        // GIVEN
        String params = "-r us-west-2 -fleet-id fleet-id -c compute_name -loc location";
        String[] args = params.split(" ");

        // WHEN
        try {
            Application.main(args);
        } catch (Exception ex) {
            // Do nothing, we're only testing that the SystemProperties are set in this test.
        }

        // THEN
        Map paramMap = new HashMap<String, String>();
        System.getProperties().forEach((k, v) -> {
            paramMap.put(k, v);
        });

        assertEquals(paramMap.get(JAVA_PROP_TTL_KEY), JAVA_PROP_TTL_VALUE);
        assertEquals(paramMap.get(JAVA_PROP_NEG_TTL_KEY), JAVA_PROP_NEG_TTL_VALUE);
        assertEquals(paramMap.get(JAVA_PROP_NETWORK_ADDR_CACHE_TTL_KEY), JAVA_PROP_NETWORK_ADDR_CACHE_TTL_VALUE);
        assertEquals(paramMap.get(JAVA_PROP_PREFER_IPV4_STACK_KEY), JAVA_PROP_PREFER_IPV4_STACK_VALUE);
        assertEquals(paramMap.get(JAVA_PROP_FORKJOINPOOL_PARALLELISM_KEY), JAVA_PROP_FORKJOINPOOL_PARALLELISM_VALUE);
        assertEquals(paramMap.get(JAVA_PROP_SECURITY_EGD_KEY), JAVA_PROP_SECURITY_EGD_VALUE);
    }

}
