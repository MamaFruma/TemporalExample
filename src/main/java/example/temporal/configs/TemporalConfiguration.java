package example.temporal.configs;

import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import com.uber.m3.tally.StatsReporter;
import example.temporal.activitiesimplementations.GreetingActivitiesImpl;
import example.temporal.workflowimplementations.GreetingWorkflowImpl;
import io.micrometer.core.instrument.Metrics;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkerOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import com.google.protobuf.Duration;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.api.namespace.v1.NamespaceConfig;
import io.temporal.api.workflowservice.v1.RegisterNamespaceRequest;
import io.temporal.api.workflowservice.v1.RegisterNamespaceResponse;
import io.temporal.api.workflowservice.v1.UpdateNamespaceRequest;
import io.temporal.api.workflowservice.v1.UpdateNamespaceResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.UUID;

import static java.time.Duration.*;

@Configuration
public class TemporalConfiguration {
    public static final String GOODBYE_QUEUE = "goodbye_queue";
    @Value("${temporal.serviceAddress}")
    private String serviceAddress;
    @Value("${temporal.namespace}")
    private String namespace;
    @Value("${temporal.workflowruntimeout}")
    private int workflowRunTimeout;
    @Value("${temporal.workflowexecutiontimeout}")
    private int workflowExecutionTimeout;
    @Value("${temporal.workflowcachesize}")
    private int workflowCacheSize;
    @Value("${temporal.workflowhostlocalpollthreadcount}")
    private int workflowHostLocalPollThreadCount;
    @Value("${temporal.maxworkflowthreadcount}")
    private int maxWorkflowThreadCount;

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        WorkerFactoryOptions workerFactoryOptions = WorkerFactoryOptions.newBuilder()
                .setWorkflowCacheSize(workflowCacheSize)
                .setWorkflowHostLocalPollThreadCount(workflowHostLocalPollThreadCount)
                .setMaxWorkflowThreadCount(maxWorkflowThreadCount)
                .build();
        return WorkerFactory.newInstance(workflowClient, workerFactoryOptions);
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        return WorkflowClient.newInstance(
                workflowServiceStubs,
                WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    }

    @Bean
    public WorkflowServiceStubs workflowServiceStubs(Scope scope) {
        return WorkflowServiceStubs.newInstance(
                WorkflowServiceStubsOptions
                        .newBuilder()
                        .setMetricsScope(scope)
                        .setTarget(serviceAddress)
                        .build());
    }

    @Bean
    @Qualifier("hello")
    public WorkflowOptions getHelloWorkflowOptions() {
        return WorkflowOptions.newBuilder()
                .setTaskQueue("hello_queue")
                .setWorkflowRunTimeout(ofSeconds(workflowRunTimeout))
                .setWorkflowExecutionTimeout(ofSeconds(workflowExecutionTimeout))
                .setWorkflowId(String.valueOf(System.nanoTime()))
                .build();
    }

    @Bean
    public Scope returnScope() {
        StatsReporter reporter = new MicrometerClientStatsReporter(Metrics.globalRegistry);
        return new RootScopeBuilder()
                .reporter(reporter)
                .reportEvery(com.uber.m3.util.Duration.ofSeconds(10));
    }

    @Slf4j
    @Component
    public static class TemporalConnection {
        @Value("${temporal.namespace}")
        private String namespace;
        @Value("${temporal.workflowretentiondays}")
        private int workflowRetentionDays;
        @Value("${temporal.workflowexecutionretentionperiod}")
        private int workflowExecutionRetentionPeriod;
        @Value("${temporal.maxconcurrentworkflowtaskexecutionsize}")
        private int maxConcurrentWorkflowTaskExecutionSize;
        @Value("${temporal.workflowpollthreadcount}")
        private int workflowPollThreadCount;
        @Value("${temporal.workflowruntimeout}")
        private int workflowRunTimeout;
        @Value("${temporal.workflowexecutiontimeout}")
        private int workflowExecutionTimeout;

        private final WorkflowServiceStubs service;
        private final WorkerFactory factory;

        public TemporalConnection(WorkflowServiceStubs workflowServiceStubs, WorkerFactory factory) {
            this.factory = factory;
            this.service = workflowServiceStubs;
        }

        private void startWorkers() {
            WorkerOptions workerOptions = WorkerOptions.newBuilder()
                    .setMaxConcurrentWorkflowTaskExecutionSize(maxConcurrentWorkflowTaskExecutionSize)
                    .setWorkflowPollThreadCount(workflowPollThreadCount)
                    .build();

            Worker helloWorker = factory.newWorker("hello_queue", workerOptions);
            helloWorker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
            helloWorker.registerActivitiesImplementations(new GreetingActivitiesImpl());

            factory.start();
        }

        private void initNamespace() throws InterruptedException {
            Duration retention = Duration.newBuilder().setSeconds(workflowExecutionRetentionPeriod).build();
            boolean connected = false;
            int counter = 0;
            while (!connected && counter < 10) {
                try {
                    log.info(
                            "Registering namespace \"{}\" with a retention period of {} days",
                            namespace,
                            workflowRetentionDays);
                    RegisterNamespaceRequest request =
                            RegisterNamespaceRequest.newBuilder()
                                    .setNamespace(namespace)
                                    .setWorkflowExecutionRetentionPeriod(retention)
                                    .build();
                    RegisterNamespaceResponse response = service.blockingStub().registerNamespace(request);

                    connected = response.isInitialized();
                    if (!response.isInitialized()) {
                        log.warn(response.getInitializationErrorString());
                    }

                } catch (StatusRuntimeException ex) {
                    if (ex.getStatus().getCode() == Status.ALREADY_EXISTS.getCode()) {
                        log.info("Domain \"{}\" already exists", namespace);
                        UpdateNamespaceRequest request =
                                UpdateNamespaceRequest.newBuilder()
                                        .setNamespace(namespace)
                                        .setConfig(
                                                NamespaceConfig.newBuilder()
                                                        .setWorkflowExecutionRetentionTtl(retention)
                                                        .build())
                                        .build();
                        UpdateNamespaceResponse response = service.blockingStub().updateNamespace(request);
                        connected = response.isInitialized();
                        log.info("Connected to server => {}", connected);
                    } else {
                        log.error("Cannot connect to Temporal service.  Waiting for 2 seconds...", ex);
                    }
                } catch (Exception e) {
                    log.error("Cannot connect to Temporal service.  Waiting for 2 seconds...", e);
                }
                Thread.sleep(2000);
                counter++;
            }
        }

        @PostConstruct
        public void init() throws InterruptedException {
            initNamespace();
            startWorkers();
            log.info("Temporal connection initialized");
        }
    }
}
