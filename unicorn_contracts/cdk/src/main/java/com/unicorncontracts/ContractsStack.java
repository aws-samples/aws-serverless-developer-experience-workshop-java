package com.unicorncontracts;



import com.unicornshared.UnicornNamespaces;
import software.amazon.awscdk.services.apigateway.ApiDefinition;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.SpecRestApi;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.resourceexplorer2.CfnView;
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
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Map.entry;
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


        final Queue contractsTableStreamToEventPipeDLQ = Queue.Builder.create(
                        this,"ContractsTableStreamToEventPipeDLQ")
                .removalPolicy(RemovalPolicy.DESTROY)
                .encryption(QueueEncryption.SQS_MANAGED)
                .retentionPeriod(Duration.days(14))
                .queueName("ContractsTableStreamToEventPipeDLQ")
                .build();

        final Role contractsTableStreamToEventPipeRole = Role.Builder.create(this,"ContractsTableStreamToEventPipeRole")
                .assumedBy(new ServicePrincipal("pipes.amazonaws.com"))
                .inlinePolicies(Map.ofEntries(
                        entry("ContractsTableStreamToEventPipePolicy", PolicyDocument.Builder.create()
                                .statements(Arrays.asList(
                                        PolicyStatement.Builder.create()
                                                .effect(Effect.ALLOW)
                                                .actions(Arrays.asList("dynamodb:ListStreams"))
                                                .resources(Arrays.asList("*"))
                                                .build(),
                                        PolicyStatement.Builder.create()
                                                .effect(Effect.ALLOW)
                                                .actions(Arrays.asList("dynamodb:DescribeStream","dynamodb:GetRecords","dynamodb:GetShardIterator"))
                                                .resources(Arrays.asList(contractsTable.getTableStreamArn()))
                                                .build(),
                                        PolicyStatement.Builder.create()
                                                .effect(Effect.ALLOW)
                                                .actions(Arrays.asList("events:PutEvents"))
                                                .resources(Arrays.asList(eventBus.getEventBusArn()))
                                                .build(),
                                        PolicyStatement.Builder.create()
                                                .effect(Effect.ALLOW)
                                                .actions(Arrays.asList("sqs:SendMessage"))
                                                .resources(Arrays.asList(contractsTableStreamToEventPipeDLQ.getQueueArn()))
                                                .build()
                                ))
                                .build())

                ))
                .build();

        final CfnPipe.FilterProperty dynamodbfilterproperty = CfnPipe.FilterProperty.builder()
                .pattern("{\"eventName\":[\"INSERT\",\"MODIFY\"],\"dynamodb\":{\"NewImage\":{\"contract_status\":{\"S\":[\"DRAFT\",\"APPROVED\"]}}}}")
                .build();
        final CfnPipe contractsTableStreamToEventPipe = CfnPipe.Builder.create(this,"contractsTableStreamToEventPipe")
                .source(contractsTable.getTableArn())
                .sourceParameters(CfnPipe.PipeSourceParametersProperty.builder()
                        .dynamoDbStreamParameters(CfnPipe.PipeSourceDynamoDBStreamParametersProperty.builder()
                                .batchSize(1)
                                .maximumRetryAttempts(3)
                                .startingPosition("LATEST")
                                .onPartialBatchItemFailure("AUTOMATIC_BISECT")

                                .deadLetterConfig(CfnPipe.DeadLetterConfigProperty.builder()
                                        .arn(contractsTableStreamToEventPipeDLQ.getQueueArn())
                                        .build())
                                .build())
                        .filterCriteria(CfnPipe.FilterCriteriaProperty.builder()
                                .filters(Arrays.asList(dynamodbfilterproperty))
                                .build())
                        .build())
                .target(eventBus.getEventBusArn())
                .targetParameters(CfnPipe.PipeTargetParametersProperty.builder()
                        .eventBridgeEventBusParameters(CfnPipe.PipeTargetEventBridgeEventBusParametersProperty.builder()
                                .source(eventBus.getEventBusArn())
                                .detailType("ContractStatusChanged")
                                .build())
                        .inputTemplate(new PipeInputTemplate().toString())
                        .build())
                .roleArn(contractsTableStreamToEventPipeRole.getRoleArn())
                .build();

    }

}

class PipeInputTemplate{
    String property_id = "<$.dynamodb.NewImage.property_id.S>";
    String contract_id= "<$.dynamodb.NewImage.contract_id.S>";
    String contract_status= "<$.dynamodb.NewImage.contract_status.S>";
    String contract_last_modified_on= "<$.dynamodb.NewImage.contract_last_modified_on.S>";

    @Override
    public String toString() {
        return "{"
                + "        \"property_id\":\"" + property_id + "\""
                + ",         \"contract_id\":\"" + contract_id + "\""
                + ",         \"contract_status\":\"" + contract_status + "\""
                + ",         \"contract_last_modified_on\":\"" + contract_last_modified_on + "\""
                + "}";
    }
}