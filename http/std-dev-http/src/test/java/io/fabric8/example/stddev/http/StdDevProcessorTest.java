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

package io.fabric8.example.stddev.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;

import java.util.List;

public class StdDevProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testProcess() throws Exception {
        RandomGenerator rg = new JDKRandomGenerator();
        double[] array = new double[10];
        ObjectMapper objectMapper = new ObjectMapper();
        for (int i = 0; i < array.length; i++) {
            array[i] = rg.nextDouble();
        }
        String body = objectMapper.writeValueAsString(array);
        SummaryStatistics summaryStatistics = new SummaryStatistics();
        List<Double> list = new ObjectMapper().readValue(body, List.class);
        for (Double value : list) {
            summaryStatistics.addValue(value);
        }
        String stdDev = Double.toString(summaryStatistics.getStandardDeviation());

        resultEndpoint.expectedBodiesReceived(stdDev);

        template.sendBody(body);

        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").process(new StdDevProcessor()).to("mock:result");
            }
        };
    }
}
