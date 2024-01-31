
package com.unicornshared;

import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;
import java.io.IOException;

import java.util.HashMap;

import org.junit.jupiter.api.Test;


public class SharedResourceStackTest {

    @Test
    public void testUnicornPropertiesNamespace()  {
        App app = new App();
        SharedResourceStack stack = new SharedResourceStack(app, "test");

        Template template = Template.fromStack(stack);



        template.hasResourceProperties("AWS::SSM::Parameter", new HashMap<String, String>() {{
            put("Name","/uni-prop/UnicornPropertiesNamespace");
            put("Value","unicorn.properties");
        }} );



    }

    @Test
    public void testUnicornWebNamespace() {
        App app = new App();
        SharedResourceStack stack = new SharedResourceStack(app, "test");

        Template template = Template.fromStack(stack);

        template.hasResourceProperties("AWS::SSM::Parameter", new HashMap<String, String>() {{
            put("Name","/uni-prop/UnicornWebNamespace");
            put("Value","unicorn.web");
        }} );




    }

    @Test
    public void testUnicornContractsNamespace()  {
        App app = new App();
        SharedResourceStack stack = new SharedResourceStack(app, "test");

        Template template = Template.fromStack(stack);



        template.hasResourceProperties("AWS::SSM::Parameter", new HashMap<String, String>() {{
            put("Name","/uni-prop/UnicornContractsNamespace");
            put("Value","unicorn.contracts");
        }} );




    }
}
