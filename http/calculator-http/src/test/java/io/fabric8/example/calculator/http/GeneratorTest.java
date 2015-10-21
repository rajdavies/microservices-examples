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

package io.fabric8.example.calculator.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

public class GeneratorTest {
    @Test
    public void testGenerator() throws Exception {

        RandomGenerator rg = new JDKRandomGenerator();
        double[] array = new double[10];

        for (int i = 0; i < array.length; i++) {
            array[i] = rg.nextDouble();
        }

        final ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(array));
    }
}
