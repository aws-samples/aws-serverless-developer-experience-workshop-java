package com.unicornshared;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.ssm.StringParameter;

public class SharedResourceStack extends Stack {
    public SharedResourceStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SharedResourceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        StringParameter.Builder.create(this,UnicornNamespaces.NameSpace.UnicornContractsNamespaceParam.name())
                .parameterName(UnicornNamespaces.NameSpace.UnicornContractsNamespaceParam.name)
                .stringValue(UnicornNamespaces.NameSpace.UnicornContractsNamespaceParam.value).build();

        StringParameter.Builder.create(this,UnicornNamespaces.NameSpace.UnicornPropertiesNamespaceParam.name())
                .parameterName(UnicornNamespaces.NameSpace.UnicornPropertiesNamespaceParam.name)
                .stringValue(UnicornNamespaces.NameSpace.UnicornPropertiesNamespaceParam.value).build();

        StringParameter.Builder.create(this,UnicornNamespaces.NameSpace.UnicornWebNamespaceParam.name())
                .parameterName(UnicornNamespaces.NameSpace.UnicornWebNamespaceParam.name)
                .stringValue(UnicornNamespaces.NameSpace.UnicornWebNamespaceParam.value).build();
    }


}
