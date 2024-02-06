package com.amazon.gamelift.agent.utils;

import com.amazon.gamelift.agent.model.exception.AgentException;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EcsMetadataReaderTest {
    private static final String TEST_CONTAINER_ID = "0206b271-b33f-47ab-86c6-a0ba208a70a9";
    private static final String JSON_BODY_WITH_CONTAINER_ARN = "{\"DockerId\": \"ea32192c8553fbff06c9340478a2ff089b2bb5646fb718b4ee206641c9086d66\","
            + "\"ContainerARN\": \"arn:aws:ecs:us-west-2:111122223333:container/" + TEST_CONTAINER_ID + "\"}";
    private static final String TEST_TASK_ID = "158d1c8083dd49d6b527399fd6414f5c";
    private static final String JSON_BODY_WITH_TASK_ARN = "{\"Cluster\": \"default\",\n"
            + "\"TaskARN\": \"arn:aws:ecs:us-west-2:111122223333:task/default/" + TEST_TASK_ID + "\"}";

    @Mock private HttpClient mockHttpClient;
    @Mock private SystemEnvironmentProvider mockSystemEnvironmentProvider;
    @Mock private HttpResponse mockHttpResponse;

    private EcsMetadataReader ecsMetadataReader;

    @BeforeEach
    public void setup() throws AgentException {
        ecsMetadataReader = new EcsMetadataReader(
                new Gson(),
                mockHttpClient,
                mockSystemEnvironmentProvider);
        when(mockSystemEnvironmentProvider.getenv(any())).thenReturn("http://169.254.170.2");
    }

    @Test
    public void GIVEN_jsonBody_WHEN_getContainerId_THEN_idReturned() throws Exception {
        // GIVEN
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.body()).thenReturn(JSON_BODY_WITH_CONTAINER_ARN);

        // WHEN
        final String containerId = ecsMetadataReader.getContainerId();

        // THEN
        assertEquals(TEST_CONTAINER_ID, containerId);
    }

    @Test
    public void GIVEN_jsonBody_WHEN_getTaskId_THEN_idReturned() throws Exception {
        // GIVEN
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.body()).thenReturn(JSON_BODY_WITH_TASK_ARN);

        // WHEN
        final String taskId = ecsMetadataReader.getTaskId();

        // THEN
        assertEquals(TEST_TASK_ID, taskId);
    }
}
