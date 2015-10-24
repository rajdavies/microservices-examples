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
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
@ContextName("Calculator")
public class CalculatorHTTP extends RouteBuilder implements Runnable {

    @Inject
    @Uri("netty4-http:http://{{service:collector:localhost:8184}}/collector")
    private Endpoint collectorService;

    @Inject
    @Uri("netty4-http:http://{{service:variance:localhost:8182}}/variance")
    private Endpoint varianceService;

    @Inject
    @Uri("netty4-http:http://{{service:std-dev:localhost:8183}}/std-dev")
    private Endpoint stdDevService;

    @Inject
    @Uri("netty4-http:http://{{service:qs-cdi-camel-jetty:localhost:8080}}/camel/hello?keepAlive=false&disconnect=true")
    private Endpoint httpEndpoint;

    public static final int NUMBER_OF_ENTRIES = 2;
    private Executor executor = Executors.newSingleThreadExecutor();

    public void run() {

        try {
            System.err.println("RUNNING");
            System.err.println(" httpEndpoint " + httpEndpoint);
            System.err.println(" STD_DEV " + stdDevService);
            System.err.println(" VARIANCE " + varianceService);
            System.err.println(" COLLECTOR " + collectorService);

            getContext().addRoutePolicyFactory(new MetricsRoutePolicyFactory());
            ProducerTemplate producerTemplate = getContext().createProducerTemplate();

            RandomGenerator rg = new JDKRandomGenerator();
            double[] array = new double[NUMBER_OF_ENTRIES];
            ObjectMapper objectMapper = new ObjectMapper();

            while (true) {
                try {
                    for (int i = 0; i < array.length; i++) {
                        array[i] = rg.nextDouble();
                    }
                    final String body = objectMapper.writeValueAsString(array);
                    Exchange exchange = producerTemplate.send("direct:start", new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
                            exchange.getIn().setBody(body);
                        }
                    });

                } catch (Throwable e) {
                    e.printStackTrace();
                    sleep(5000);
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void configure() throws Exception {
        onException(Throwable.class).maximumRedeliveries(-1).delay(5000);
        from("direct:start")
            .multicast()
            .parallelProcessing().timeout(500).to(stdDevService, varianceService)
            .end().setBody(body().append("HTTP FINISHED")).to(collectorService);
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


