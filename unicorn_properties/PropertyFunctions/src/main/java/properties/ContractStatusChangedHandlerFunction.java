package properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import properties.helper.PropertyHelper;
import schema.unicorn_contracts.contractstatuschanged.AWSEvent;
import schema.unicorn_contracts.contractstatuschanged.ContractStatusChanged;
import schema.unicorn_contracts.contractstatuschanged.marshaller.Marshaller;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;

/**
 * Lambda handler to update the contract status change
 */
public class ContractStatusChangedHandlerFunction {

        Logger logger = LogManager.getLogger();

        final String TABLE_NAME = System.getenv("CONTRACT_STATUS_TABLE");

        PropertyHelper helper = new PropertyHelper(TABLE_NAME);
        ObjectMapper objectMapper = new ObjectMapper();

        /**
         * 
         * @param inputStream
         * @param outputStream
         * @param context
         * @return
         * @throws IOException
         * 
         */
        @Tracing
        @Metrics(captureColdStart = true)
        @Logging(logEvent = true)
        public void handleRequest(InputStream inputStream, OutputStream outputStream,
                        Context context) throws IOException {

                // deseralised and save contract status change in dynamodb table
                // String strInput = new String(inputStream.readAllBytes());
                AWSEvent<ContractStatusChanged> event = Marshaller.unmarshalEvent(inputStream,
                                ContractStatusChanged.class);
                // save to database
                saveContractStatus(event.getDetail());

                OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                writer.write(objectMapper.writeValueAsString(event.getDetail()));
                writer.close();
        }

        @Tracing
        void saveContractStatus(ContractStatusChanged event) {
                helper.saveContractStatus(event);
        }

        public void setHelper(PropertyHelper helper) {
                this.helper = helper;
        }
}
