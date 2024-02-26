package com.unicorncontracts;

import com.unicornshared.UnicornStage;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class ContractsApp {
    public static void main(final String[] args) {
        App app = new App();

        new ContractsStack(app, "UnicornContracts", ContractsStack.ContractStackProps.builder()
                .stage(UnicornStage.Stage.local)
                .build());
        app.synth();
    }
}

