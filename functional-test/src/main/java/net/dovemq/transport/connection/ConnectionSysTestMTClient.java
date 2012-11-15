/**
 * Copyright 2012 Tejeswar Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.dovemq.transport.connection;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.dovemq.transport.common.CAMQPTestTask;

public class ConnectionSysTestMTClient
{
    private static int NUM_THREADS = 5;
    private static String brokerContainerId = null;
    private static final Random r = new Random();

    private static class TestConnector extends CAMQPTestTask implements Runnable
    {
        public TestConnector(CountDownLatch startSignal,
                CountDownLatch doneSignal)
        {
            super(startSignal, doneSignal);
        }

        @Override
        public void run()
        {
            try
            {
                Thread.sleep(r.nextInt(100));
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            CAMQPConnectionProperties connectionProps = CAMQPConnectionProperties.createConnectionProperties();
            CAMQPConnection connection = CAMQPConnectionFactory.createCAMQPConnection(brokerContainerId, connectionProps);
            if (connection == null)
                System.out.println("AMQP connection could not be created");
            else
                System.out.println("AMQP connection created: " + connection.getKey());

            waitForReady();

            try
            {
                Thread.sleep(2000 + r.nextInt(500));
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connection.close();

            try
            {
                Thread.sleep(500 + r.nextInt(500));
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            done();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        Thread.sleep(500 + r.nextInt(500));
        String id = (args.length == 0)? "publisher" : args[0];
        CAMQPConnectionManager.initialize(id);
        System.out.println("AMQP client container ID: " + CAMQPConnectionManager.getContainerId());

        brokerContainerId = String.format("broker@%s", args[1]);
        NUM_THREADS = Integer.parseInt(args[2]);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(NUM_THREADS);

        TestConnector[] senders = new TestConnector[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++)
        {
            TestConnector connector = new TestConnector(startSignal, doneSignal);
            senders[i] = connector;
            executor.submit(connector);
        }

        ConnectionCommandMBean command = new ConnectionCommand();
        while (true)
        {
            Collection<String> openConnections = command.list();
            if (openConnections.size() == NUM_THREADS)
            {
                break;
            }
            Thread.sleep(2000);
        }

        startSignal.countDown();
        doneSignal.await();
        Thread.sleep(5000);

        executor.shutdown();

        CAMQPConnectionManager.shutdown();
        CAMQPConnectionFactory.shutdown();

    }
}
