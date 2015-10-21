/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.example.calculator.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

public class CalculatorHTTP {

    public static final int NUMBER_OF_ENTRIES = 100;

    public static void main(String[] args) throws Exception {

        String stddevHost = System.getenv("STD_DEV_HTTP_SERVICE_HOST");
        String stddevPort = System.getenv("STD_DEV_HTTP_SERVICE_PORT");
        String collectorService = "http://localhost:8180/collector";
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.addRoutePolicyFactory(new MetricsRoutePolicyFactory());
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        camelContext.setStreamCaching(false);
        camelContext.start();

        RandomGenerator rg = new JDKRandomGenerator();
        double[] array = new double[NUMBER_OF_ENTRIES];
        ObjectMapper objectMapper = new ObjectMapper();

        while (true) {
            try {

                for (int i = 0; i < array.length; i++) {
                    array[i] = rg.nextDouble();
                }

                final String body = objectMapper.writeValueAsString(array);
                Exchange exchange = producerTemplate.request(stddevService, new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
                        exchange.getIn().setBody(body);
                    }
                });

                System.out.println("EXCHANGE " + exchange.getOut().getBody(String.class));

                producerTemplate.sendBody(collectorService, "finished");

            } catch (Throwable e) {
                e.printStackTrace();
                Thread.sleep(5000);
            }
        }
    }
}
