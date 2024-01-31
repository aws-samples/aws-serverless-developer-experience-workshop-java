/*
package com.unicornshared;

import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;
import java.io.IOException;

import java.util.HashMap;

import org.junit.jupiter.api.Test;


public class SharedResourceStackTest {

    @Test
    public void testStack() throws IOException {
        App app = new App();
        SharedResourceStack stack = new SharedResourceStack(app, "test");

        Template template = Template.fromStack(stack);

        template.hasResourceProperties("AWS::SSM::Parameter", new HashMap<String, HashMap<String,String>>() {{
          put("Properties",new HashMap<String,String>(){{put("Name","/uni-prop/UnicornWebNamespace");}} );
        }});

    }
}
*/