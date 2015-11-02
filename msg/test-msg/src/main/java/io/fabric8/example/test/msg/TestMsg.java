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
package io.fabric8.example.test.msg;

import io.fabric8.example.common.msg.Variables;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
@ContextName("Calculator")
public class TestMsg extends RouteBuilder implements Runnable {

    public static final int NUMBER_OF_ENTRIES = 2;
    @Inject
    @Uri("netty4-http:http://{{service:collector-http:localhost:8184}}/collector")
    private Endpoint collectorService;
    @Inject
    @Uri("netty4-http:http://{{service:variance-http:localhost:8182}}/variance")
    private Endpoint varianceService;
    @Inject
    @Uri("netty4-http:http://{{service:std-dev-http:localhost:8183}}/std-dev")
    private Endpoint stdDevService;
    @Inject
    @Uri("netty4-http:http://{{service:qs-cdi-camel-jetty:localhost:8080}}/camel/hello?keepAlive=false&disconnect=true")
    private Endpoint httpEndpoint;
    private Executor executor = Executors.newSingleThreadExecutor();

    public void run() {
        try {
            doTest();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected void doTest() throws Exception {

        //String msgURI = "tcp://" + getEnv("FABRIC8MQ_SERVICE_HOST", "localhost") + ":" + getEnv("FABRIC8MQ_SERVICE_PORT", "61616");
        String msgURI = Variables.MSG_URL;
        System.err.println("CONNECTING TO " + msgURI);
        PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
        connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory("failover:(" + msgURI + ")"));

        final AtomicInteger counter = new AtomicInteger();
        final CamelContext serviceContext = new DefaultCamelContext();
        serviceContext.addComponent("jms",
                                       JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        serviceContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                RandomGenerator rg = new JDKRandomGenerator();
                int num = rg.nextInt();
                from("jms:topic:test")
                    .process(new Producer("FOO", serviceContext.createProducerTemplate()));

                from("jms:topic:test")
                    .process(new Producer("BAR", serviceContext.createProducerTemplate()));

                /*
                from("jms:queue:myQueue").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        System.err.println("GOT A REPLY(" + exchange.getIn().getBody() + "!!! " + exchange.getIn().getHeaders());
                    }
                });
                */

                from("jms:queue:myQueue").aggregate(header("CORRELATE"), new AggregationStrategy() {
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
                }).completionSize(2).completionTimeout(2000).process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        System.err.println("YAY GOT RESULT " + exchange.getIn().getBody());
                    }
                });
            }
        });
        serviceContext.start();

        CamelContext requestorContext = new DefaultCamelContext();
        requestorContext.addComponent("jms",
                                         ActiveMQComponent.jmsComponentAutoAcknowledge(connectionFactory));

        requestorContext.start();

        ProducerTemplate producerTemplate = requestorContext.createProducerTemplate();
        for (int i = 0; i < 1000; i++) {
            /*
            Object response = producerTemplate
                                  .requestBodyAndHeader(
                                                           "jms:myQueue.queue?exchangePattern=InOut&requestTimeout=40000&timeToLive=40000"
                                                               + "&asyncConsumer=true&asyncStartListener=true&concurrentConsumers=10"
                                                               + "&useMessageIDAsCorrelationID=true",
                                                           "mBodyMsg", "HeaderString", "HeaderValue");

            System.err.println("RESPONSE = " + response);
            */
            String body = "TEST " + i;
            System.err.println("SENT MESSAGE " + body);
            String endPoint = "jms:topic:test?preserveMessageQos=true&replyTo=myQueue&replyToType=Exclusive"
                                  + "&asyncConsumer=true&asyncStartListener=true&concurrentConsumers=10";
            producerTemplate.sendBody(endPoint, body);
            Thread.sleep(1000);
        }

        requestorContext.stop();
        serviceContext.stop();
    }

    @Override
    public void configure() throws Exception {
        onException(Throwable.class).maximumRedeliveries(-1).delay(5000);
        from("direct:start")
            .to(stdDevService)
            .to("log:results?showAll=true&multiline=true");

    }


    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class Producer implements Processor {
        private final String name;
        private final ProducerTemplate producerTemplate;

        Producer(String name, ProducerTemplate template) {
            this.name = name;
            this.producerTemplate = template;
            this.producerTemplate.setDefaultEndpointUri("log:failed");
        }

        public void process(Exchange exchange) throws Exception {

            System.err.println(name + "  GOT " + exchange.getIn().getBody());
            System.err.println(exchange.getIn().getHeaders());
            exchange.getIn().setBody(name + " RESPONSE " + exchange.getIn().getBody());
            ActiveMQDestination replyTo = exchange.getIn().getHeader("JMSReplyTo", ActiveMQDestination.class);
            final String messageId = exchange.getIn().getHeader("JMSMessageID", String.class);

            if (replyTo != null) {
                Exchange copy = new DefaultExchange(exchange);
                copy.setPattern(ExchangePattern.InOnly);
                copy.getIn().setHeader("CORRELATE", messageId);
                copy.getIn().setBody("SENT REPLY TO " + messageId);
                producerTemplate.send("jms:queue:" + replyTo.getPhysicalName(), copy);

            }
        }
    }
}
