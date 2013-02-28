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

package net.dovemq.transport.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.dovemq.transport.common.CAMQPTestTask;
import net.dovemq.transport.connection.CAMQPConnectionFactory;
import net.dovemq.transport.connection.CAMQPConnectionManager;
import net.dovemq.transport.endpoint.CAMQPSourceInterface;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkSenderInterface;
import net.dovemq.transport.link.CAMQPMessage;
import net.dovemq.transport.protocol.CAMQPEncoder;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

import org.jboss.netty.buffer.ChannelBuffer;

final class FileHeader implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public FileHeader(String fileName, long fileSize) {
        super();
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    private final String fileName;

    private final long fileSize;
}

final class MockLinkSender implements CAMQPLinkSenderInterface {
    @Override
    public void sendMessage(CAMQPMessage message) {
        // TODO Auto-generated method stub
    }

    @Override
    public void messageSent(CAMQPControlTransfer transferFrame) {
        // TODO Auto-generated method stub
    }

    @Override
    public void registerSource(CAMQPSourceInterface source) {
        // TODO Auto-generated method stub
    }

    @Override
    public long getHandle() {
        // TODO Auto-generated method stub
        return 0;
    }
}

final class SessionCreator extends CAMQPTestTask implements Runnable {
    public SessionCreator(String brokerContainerId,
            SessionCommand localSessionCommand,
            CountDownLatch startSignal,
            CountDownLatch doneSignal) {
        super(startSignal, doneSignal);
        this.brokerContainerId = brokerContainerId;
        this.localSessionCommand = localSessionCommand;
    }

    @Override
    public void run() {
        waitForReady();

        localSessionCommand.sessionCreate(brokerContainerId);
        System.out.println("created session in thread id: " + Thread.currentThread()
                .getId());
        done();
    }

    private final String brokerContainerId;

    private final SessionCommand localSessionCommand;
}

final class SessionSender extends CAMQPTestTask implements Runnable {
    public SessionSender(CAMQPSession session,
            CountDownLatch startSignal,
            CountDownLatch doneSignal) {
        super(startSignal, doneSignal);
        this.session = session;
    }

    @Override
    public void run() {
        waitForReady();

        try {
            String targetName = String.format("foo2-%d.txt",
                    Thread.currentThread().getId());
            SessionIOTestUtils.transmitBinaryData(session, targetName);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        done();
    }

    private final CAMQPSession session;
}

final class SessionIOTestUtils {
    private static final long TOTAL_BYTES_TO_SEND = 8388608;

    static void createSessions(int numThreads,
            String brokerContainerId,
            SessionCommand sessionCommand) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            SessionCreator sessionCreator = new SessionCreator(brokerContainerId,
                    sessionCommand,
                    startSignal,
                    doneSignal);
            executor.submit(sessionCreator);
        }

        startSignal.countDown();
        doneSignal.await();
        Thread.sleep(500);
        executor.shutdown();
    }

    static void sendTransferFrames(int numThreads,
            String brokerContainerId,
            SessionCommand sessionCommand) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numThreads);

        Collection<Integer> attachedChannels = sessionCommand.getChannelId(brokerContainerId);

        for (int channelId : attachedChannels) {
            CAMQPSession session = CAMQPSessionManager.getSession(brokerContainerId,
                    channelId);
            SessionSender sessionSender = new SessionSender(session,
                    startSignal,
                    doneSignal);
            executor.submit(sessionSender);
        }

        startSignal.countDown();
        doneSignal.await();
        Thread.sleep(500);
        executor.shutdown();
    }

    static void closeSessions(int numThreads,
            String brokerContainerId,
            SessionCommand sessionCommand) throws InterruptedException {
        Collection<Integer> attachedChannels = sessionCommand.getChannelId(brokerContainerId);

        for (int channelId : attachedChannels) {
            sessionCommand.sessionClose(brokerContainerId, channelId);
            System.out.println("closed session id: " + channelId);
            Thread.sleep(500);
        }
    }

    static byte[] marshalFileHeader(String target) throws IOException {
        ObjectOutputStream oos = null;
        try {
            FileHeader fileHeader = new FileHeader(target, TOTAL_BYTES_TO_SEND);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(fileHeader);
            return bos.toByteArray();
        } finally {
            if (oos != null)
                oos.close();
        }
    }

    static FileHeader unmarshalFileHeader(byte[] buf) throws IOException,
            ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bis);
        FileHeader fileHeader = (FileHeader) ois.readObject();
        ois.close();
        return fileHeader;
    }

    protected static void transmitBinaryData(CAMQPSession session, String target) throws InterruptedException,
            IOException {
        int bufsizes[] = new int[] { 64, 128, 256, 512, 1024, 2048 };
        Random randomGenerator = new Random();
        boolean firstTime = true;
        long deliveryId = 0;
        long linkHandle = 1;

        ChannelBuffer attachedBuffer = createAttachedFrame(linkHandle);
        session.sendLinkControlFrame(attachedBuffer);

        MockLinkSender linkSender = new MockLinkSender();

        long dataSent = 0;
        while (dataSent < TOTAL_BYTES_TO_SEND) {
            int bufsizeIndex = 0;
            byte[] inbuf = null;
            if (firstTime) {
                firstTime = false;
                inbuf = marshalFileHeader(target);
            } else {
                bufsizeIndex = randomGenerator.nextInt(6);
                int dataToSend = bufsizes[bufsizeIndex];
                if (TOTAL_BYTES_TO_SEND - dataSent < dataToSend) {
                    dataToSend = (int) (TOTAL_BYTES_TO_SEND - dataSent);
                }
                inbuf = new byte[dataToSend];
                randomGenerator.nextBytes(inbuf);
                dataSent += dataToSend;
            }

            deliveryId = session.getNextDeliveryId();
            CAMQPControlTransfer transferFrame = createTransferFrame(linkHandle,
                    deliveryId);
            session.sendTransfer(transferFrame,
                    new CAMQPMessagePayload(inbuf),
                    linkSender);

            int randomInt = randomGenerator.nextInt(10);
            Thread.sleep(randomInt);
        }
        System.out.println("Delivery ID of last message: " + deliveryId
                + " target: "
                + target);
        Thread.sleep(1000);
    }

    private static CAMQPControlTransfer createTransferFrame(long linkHandle,
            long deliveryId) {
        CAMQPControlTransfer transfer = new CAMQPControlTransfer();
        transfer.setDeliveryId(deliveryId);
        transfer.setDeliveryTag(UUID.randomUUID().toString().getBytes());
        transfer.setHandle(linkHandle);
        transfer.setMessageFormat(1L);
        transfer.setMore(false);
        return transfer;
    }

    private static ChannelBuffer createAttachedFrame(long linkHandle) {
        CAMQPControlAttach attachControl = new CAMQPControlAttach();
        attachControl.setHandle(linkHandle);
        attachControl.setName("TestLink");
        attachControl.setMaxMessageSize(BigInteger.valueOf(16384L));

        CAMQPEncoder encoder = CAMQPEncoder.createCAMQPEncoder();
        CAMQPControlAttach.encode(encoder, attachControl);
        return encoder.getEncodedBuffer();
    }

    public static void cleanup() {
        CAMQPSessionManager.shutdown();
        CAMQPConnectionManager.shutdown();
        CAMQPConnectionFactory.shutdown();
    }
}
