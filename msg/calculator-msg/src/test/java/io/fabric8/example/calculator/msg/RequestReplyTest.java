/*
 *
 *  * Copyright 2005-2015 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */

package io.fabric8.example.calculator.msg;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class RequestReplyTest {

    private MessageBroker broker;

    @Test
    public void theTest() throws Exception{

        PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
        connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory("failover:(tcp://localhost:61616)"));

        final AtomicInteger counter = new AtomicInteger();
        CamelContext serviceContext = new DefaultCamelContext();
        serviceContext.addComponent("jms",
                                JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));


        serviceContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                RandomGenerator rg = new JDKRandomGenerator();
                int num = rg.nextInt();
                from("jms:myQueue.queue")
                    // .setHeader("JMSMessageID", constant("ID : " + num))
                    //  .setHeader("JMSReplyTo", constant("myQueue.queue"))
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            String body = exchange.getIn().getBody(String.class);
                            /***
                             * Process data and get the response and set the resposen to the Exchage
                             * body.
                             */
                            exchange.getOut().setBody("RESPONSE " + counter.incrementAndGet());
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
            Object response = producerTemplate
                                  .requestBodyAndHeader(
                                                           "jms:myQueue.queue?exchangePattern=InOut&requestTimeout=40000&timeToLive=40000"
                                                               + "&asyncConsumer=true&asyncStartListener=true&concurrentConsumers=10"
                                                               + "&useMessageIDAsCorrelationID=true",
                                                           "mBodyMsg", "HeaderString", "HeaderValue");

            System.err.println("RESPONSE = " + response);
        }

        requestorContext.stop();
        serviceContext.stop();
    }

    @Before
    public void setUp() throws Exception{
        broker = new MessageBroker();
        broker.start();
    }

    @After
    public void tearDown() throws Exception{
        if (broker != null){
            broker.stop();
            broker=null;
        }
    }


}
