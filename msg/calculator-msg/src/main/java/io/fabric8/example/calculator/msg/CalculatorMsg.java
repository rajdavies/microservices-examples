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
package io.fabric8.example.calculator.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.example.common.msg.Variables;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@ContextName("Calculator")
public class CalculatorMsg extends RouteBuilder implements Runnable {

    @Inject
    @Uri("netty4-http:http://{{service:collector-http:localhost:8184}}/results/msg")
    private Endpoint collectorService;

    public void run() {

        try {
            System.err.println("RUNNING");
            System.err.println("AMQBROKER = " + Variables.MSG_URL);
            System.err.println(" COLLECTOR = " + collectorService);

            getContext().addRoutePolicyFactory(new MetricsRoutePolicyFactory());
            ProducerTemplate producerTemplate = getContext().createProducerTemplate();

            RandomGenerator rg = new JDKRandomGenerator();
            double[] array = new double[Variables.NUMBER_OF_ENTRIES];
            ObjectMapper objectMapper = new ObjectMapper();

            Endpoint jmsSender = getContext().getEndpoint("jms:topic:"
                                                              + Variables.CALCULATION_TOPIC
                                                              + "?preserveMessageQos=true"
                                                              + "&replyTo=" + Variables.RESULT_QUEUE
                                                              +"&replyToType=Exclusive"
                                                              + "&asyncConsumer=true"
                                                              + "&asyncStartListener=true"
                                                              + "&concurrentConsumers=10");

            while (true) {
                try {
                    for (int i = 0; i < array.length; i++) {
                        array[i] = rg.nextDouble();
                    }
                    final String body = objectMapper.writeValueAsString(array);
                    producerTemplate.sendBody(jmsSender, body);

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
        PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
        String msgURI = Variables.MSG_URL;
        connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory("failover:(" + msgURI + ")"));

        JmsComponent jms = (JmsComponent) getContext().getComponent("jms");
        jms.getConfiguration().setConnectionFactory(connectionFactory);

        from("jms:queue:"+Variables.RESULT_QUEUE).aggregate(header(Variables.CORRELATION_HEADER), new AggregationStrategy() {
            @Override
            public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                if (oldExchange == null) {
                    return newExchange;
                }

                String oldBody = oldExchange.getIn().getBody(String.class);
                String newBody = newExchange.getIn().getBody(String.class);
                oldExchange.getIn().setBody(oldBody + "+" + newBody);
                return oldExchange;
            }
        }).completionSize(Variables.NUMBER_OF_SERVICES).completionTimeout(2000)
            .setHeader("name", constant("MSG")).to(collectorService);
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


