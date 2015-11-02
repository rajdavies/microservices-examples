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

package io.fabric8.example.stddev.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.fabric8.example.common.msg.Variables;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import javax.inject.Singleton;
import java.util.List;

@Singleton
public class StdDevProcessor implements Processor {

    private final ProducerTemplate producerTemplate;

    StdDevProcessor(ProducerTemplate producerTemplate){
        this.producerTemplate=producerTemplate;
        this.producerTemplate.setDefaultEndpointUri("log:failed");
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        System.err.println("STD DEV GOT EXCHANGE " + exchange);
        String message = exchange.getIn().getBody(String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        List<Double> values = objectMapper.readValue(message, typeFactory.constructCollectionType(List.class, Double.class));
        SummaryStatistics summaryStatistics = new SummaryStatistics();
        List<Double> list = new ObjectMapper().readValue(message, List.class);
        for (Double value : list) {
            summaryStatistics.addValue(value);
        }
        String stdDev = Double.toString(summaryStatistics.getStandardDeviation());

        ActiveMQDestination replyTo = exchange.getIn().getHeader("JMSReplyTo", ActiveMQDestination.class);
        final String messageId = exchange.getIn().getHeader("JMSMessageID", String.class);

        if (replyTo != null) {
            Exchange copy = new DefaultExchange(exchange);
            copy.setPattern(ExchangePattern.InOnly);
            copy.getIn().setHeader(Variables.CORRELATION_HEADER, messageId);
            copy.getIn().setBody(stdDev);
            producerTemplate.send("jms:queue:" + replyTo.getPhysicalName(), copy);
        }
    }
}
