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

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;


public class MessageBroker extends ServiceSupport {
    private BrokerService brokerService;


    @Override
    protected void doStart() throws Exception {
        brokerService = new BrokerService();
        brokerService.setBrokerName("Test");
        brokerService.setPersistent(false);
        brokerService.addConnector("amqp://0.0.0.0:5672");
        brokerService.addConnector("tcp://0.0.0.0:61616");
        brokerService.start();
        brokerService.waitUntilStarted();
    }

    @Override
    protected void doStop(ServiceStopper serviceStopper) throws Exception {
        if (brokerService != null){
            brokerService.stop();
            brokerService.waitUntilStopped();
            brokerService = null;
        }
    }


    public static void main(String[] args){
        try {

            MessageBroker messageBroker = new MessageBroker();
            messageBroker.start();
            waitUntilStop();

        }catch(Throwable e){
            e.printStackTrace();
        }
    }

    protected static void waitUntilStop() {
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
