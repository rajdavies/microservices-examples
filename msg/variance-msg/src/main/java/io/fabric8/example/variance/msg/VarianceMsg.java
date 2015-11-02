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
package io.fabric8.example.variance.msg;

import io.fabric8.example.common.msg.Variables;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.jms.JmsComponent;

@ContextName("varianceCamel")
public class VarianceMsg extends RouteBuilder {

    VarianceProcessor processor;


    @Override
    public void configure() throws Exception {
        String msgURI = Variables.MSG_URL;
        System.err.println("MESSAGING IS " + msgURI);
        PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
        connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory("failover:(" + msgURI + ")"));

        processor = new VarianceProcessor(getContext().createProducerTemplate());

        getContext().addComponent("jms",
                                     JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        from("jms:topic:" + Variables.CALCULATION_TOPIC).process(processor);
    }
}
