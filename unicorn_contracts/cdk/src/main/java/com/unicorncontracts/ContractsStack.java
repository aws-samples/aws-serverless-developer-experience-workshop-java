package com.unicorncontracts;



import com.unicornshared.UnicornNamespaces;
import software.amazon.awscdk.services.apigateway.ApiDefinition;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.SpecRestApi;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.StreamViewType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableEncryption;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.QueueEncryption;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.pipes.CfnPipe;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

public class ContractsStack extends Stack {

    private final String ProjectName = "AWS Serverless Developer Experience";
    public ContractsStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ContractsStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


        final EventBus eventBus = EventBus.Builder.create(this,"UnicornContractsEventBus").build();
        final StringParameter unicornContractsEventBusNameParam = StringParameter.Builder.create(this,"UnicornContractsEventBusNameParam")
                .parameterName("/uni-prop/UnicornContractsEventBus")
                .stringValue(eventBus.getEventBusName())
                .build();

        final StringParameter unicornContractsEventBusArnParam = StringParameter.Builder.create(this,"UnicornContractsEventBusArnParam")
                .parameterName("/uni-prop/UnicornContractsEventBusArn")
                .stringValue(eventBus.getEventBusArn())
                .build();




        final Queue unicornContractsIngestQueue = Queue.Builder.create(
                        this,"UnicornContractsIngestQueue")
                .removalPolicy(RemovalPolicy.DESTROY)
                .encryption(QueueEncryption.SQS_MANAGED)
                .retentionPeriod(Duration.days(14))
                .queueName("UnicornContractsIngestQueue")
                .build();
        Tags.of(unicornContractsIngestQueue).add("project",ProjectName);
        Tags.of(unicornContractsIngestQueue).add("namespace",
                UnicornNamespaces.NameSpace.UnicornPropertiesNamespaceParam.value);

        final DeadLetterQueue unicornContractsIngestDLQ = DeadLetterQueue.builder()
                .queue(unicornContractsIngestQueue)
                .maxReceiveCount(1)
                .build();







        final Table contractsTable = Table.Builder.create(this,"ContractsTable")
                .removalPolicy(RemovalPolicy.DESTROY)
                .partitionKey(Attribute.builder()
                        .name("property_id")
                        .type(AttributeType.STRING)
                        .build())
                .encryption(TableEncryption.DEFAULT)
                .stream(StreamViewType.NEW_AND_OLD_IMAGES)
                .build();
        Tags.of(contractsTable).add("project",ProjectName);
        Tags.of(contractsTable).add("namespace",
                UnicornNamespaces.NameSpace.UnicornPropertiesNamespaceParam.value);


        List<String> contractsFunctionPackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                        "export AWS_REGION=us-west-2 "+
                        "&& mvn clean install " +
                        "&& cp /asset-input/target/ContractsModule-1.0.jar /asset-output/"
        );

        final Function contractEventHandlerFunction = Function.Builder.create(this,"ContractEventHandlerFunction")
                .runtime(Runtime.JAVA_21)
                .handler("contracts.ContractEventHandler::handleRequest")
                .code(Code.fromAsset("../ContractsFunction/",AssetOptions.builder()
                        .bundling(BundlingOptions.builder()
                                .image(Runtime.JAVA_21.getBundlingImage())
                                .volumes(singletonList(
                                        // Mount local .m2 repo to avoid download all the dependencies again inside the container
                                        DockerVolume.builder()
                                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                                .containerPath("/root/.m2/")
                                                .build()
                                ))
                                .user("root")
                                .outputType(ARCHIVED)
                                .command(contractsFunctionPackagingInstructions).build())
                        .build()))
                .build();

        contractsTable.grantReadWriteData(contractEventHandlerFunction);

        Tags.of(contractsTable).add("project",ProjectName);
        Tags.of(contractsTable).add("namespace",
                UnicornNamespaces.NameSpace.UnicornPropertiesNamespaceParam.value);


        contractEventHandlerFunction.addEventSource(SqsEventSource.Builder
                .create(unicornContractsIngestQueue)
                .build());


        final SpecRestApi unicornContractsApi = SpecRestApi.Builder.create(this,"UnicornContractsApi")
                .apiDefinition(ApiDefinition.fromAsset("../api.yaml"))
                .endpointTypes(Arrays.asList(EndpointType.REGIONAL))
                .build();





    }
}
