package com.dashboard.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class BatchService {

    public void runBatchOnEKS(String batchType) throws Exception {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        BatchV1Api batchApi = new BatchV1Api();

        V1Job job = new V1Job()
            .metadata(new V1ObjectMeta()
                .name("dashboard-batch-" + batchType + "-" + System.currentTimeMillis())
                .namespace("default"))
            .spec(new V1JobSpec()
                .template(new V1PodTemplateSpec()
                    .spec(new V1PodSpec()
                        .containers(Collections.singletonList(
                            new V1Container()
                                .name("batch-container")
                                .image("058264125602.dkr.ecr.us-east-1.amazonaws.com/dashboard-repo:latest")
                                .args(Collections.singletonList(batchType))
                        ))
                        .restartPolicy("Never"))));

        batchApi.createNamespacedJob("default", job, null, null, null, null);
        System.out.println("Batch job " + batchType + " submitted to EKS");
    }
}