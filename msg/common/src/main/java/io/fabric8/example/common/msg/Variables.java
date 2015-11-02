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
package io.fabric8.example.common.msg;

public class Variables {
    public static final String CALCULATION_TOPIC = "calculator.broadcast";
    public static final String RESULT_QUEUE = "calculator.results";
    public static final String CORRELATION_HEADER = "calculatorCorrelation";
    public static final int NUMBER_OF_ENTRIES = 100;
    public static final int NUMBER_OF_SERVICES = 2;
    public static final String BROKER_SERVICE = "amqbroker";


    public static final String MSG_URL;

    static{

        String str = "tcp://";
        String host = System.getenv(BROKER_SERVICE.toUpperCase() + "_SERVICE_HOST");
        if (host == null || host.isEmpty()){
            host = "localhost";
        }
        String port = System.getenv(BROKER_SERVICE.toUpperCase() + "_SERVICE_PORT");
        if (port == null || port.isEmpty()){
            port = "61616";
        }
        MSG_URL = "tcp://" + host + ":" + port;
    }
}
