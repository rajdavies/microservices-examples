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
package io.fabric8.example.stddev.http;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.impl.DefaultCamelContext;

import java.util.Map;

public class StdDevHTTP {

    public static void main(String[] args) throws Exception {

        Map<String,String> envs = System.getenv();
        System.err.println("ENVS ARE:");
        for (Map.Entry<String,String>entry:envs.entrySet()){
            System.err.println(entry.getKey() + " = " + entry.getValue());
        }

        final Processor processor = new StdDevProcessor();
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.addRoutePolicyFactory(new MetricsRoutePolicyFactory());
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jetty:http://localhost:8181/stddev").doTry().process(processor).doCatch(Throwable.class)
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                    .setBody(constant("{\"error\" : \"Service failed\"}"))
                    .end();
            }
        });

        camelContext.start();

        final Object lock = new Object();
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
}
