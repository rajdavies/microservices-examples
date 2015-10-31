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
package io.fabric8.example.stddev.msg;

import io.fabric8.annotations.ServiceName;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.jms.JmsComponent;

import javax.inject.Inject;

@ContextName("stddevCamel")
public class StdDevMsg extends RouteBuilder {

    @Inject
    StdDevProcessor processor;

    @Inject
    @ServiceName("fabric8mq")
    String msgURI;

    @Override
    public void configure() throws Exception {

        PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
        connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory(msgURI));

        getContext().addComponent("jms",
                                     JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        onException(Throwable.class).maximumRedeliveries(-1).delay(5000);
        from("jms:std-dev").process(processor);
    }
}
