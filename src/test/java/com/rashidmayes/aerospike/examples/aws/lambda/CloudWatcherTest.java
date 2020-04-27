package com.rashidmayes.aerospike.examples.aws.lambda;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class CloudWatcherTest {

    @BeforeClass
    public static void createInput() throws IOException {

    	
    }

    /*
    private Context createContext() {
        TestContext ctx = new TestContext();
        ctx.setFunctionName("Your Function Name");

        return ctx;
    }*/

    @Test
    public void testCloudWatcher() {
    	
    	
        //CloudWatcher handler = new CloudWatcher();
        //Context ctx = createContext();

        //String output = handler.handleRequest(null, ctx);

        // TODO: validate output here if needed.
        //Assert.assertEquals("Hello from Lambda!", output);
    }
}
