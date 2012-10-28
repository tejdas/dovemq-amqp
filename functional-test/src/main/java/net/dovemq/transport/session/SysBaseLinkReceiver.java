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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import net.dovemq.transport.common.CAMQPFunctionalTestUtils;
import net.dovemq.transport.frame.CAMQPMessagePayload;
import net.dovemq.transport.link.CAMQPLinkMessageHandler;
import net.dovemq.transport.protocol.data.CAMQPControlAttach;
import net.dovemq.transport.protocol.data.CAMQPControlDetach;
import net.dovemq.transport.protocol.data.CAMQPControlFlow;
import net.dovemq.transport.protocol.data.CAMQPControlTransfer;

public abstract class SysBaseLinkReceiver implements CAMQPLinkMessageHandler
{
    private SysTestCommandReceiverFactory factory = null;
    protected final CAMQPSessionInterface session;
    private FileOutputStream outputStream = null;
    private boolean firstCommand = true;
    private long expectedBytes = 0;
    private long receivedBytes = 0;

    private volatile boolean isDone = false;

    public boolean isDone()
    {
        return isDone;
    }

    void registerFactory(SysTestCommandReceiverFactory factory)
    {
        this.factory = factory;
    }

    public SysBaseLinkReceiver(CAMQPSessionInterface session)
    {
        super();
        this.session = session;
    }

    @Override
    public void sessionClosed()
    {
        if (outputStream != null)
        {
            try
            {
                outputStream.close();
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (factory != null)
            factory.remove(this);
    }

    @Override
    public void transferReceived(long transferId, CAMQPControlTransfer transferFrame, CAMQPMessagePayload payload)
    {
        try
        {
            byte[] payloadBytes = CAMQPFunctionalTestUtils.getBytes(payload);
            parsePayload(payloadBytes);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //if (transferId % 100 == 0)
          //  System.out.println("transferReceived : " + transferId);
    }

    private void parsePayload(byte[] payload) throws IOException, ClassNotFoundException
    {
        if (firstCommand)
        {
            firstCommand = false;
            FileHeader fh = SessionIOTestUtils.unmarshalFileHeader(payload);
            expectedBytes = fh.getFileSize();
            System.out.println("Target fileName: " + fh.getFileName());
            System.out.println("Target fileSize: " + expectedBytes);

            String localFileName = SessionIOTestUtils.convertToLocalFileName(fh.getFileName());
            try
            {
                outputStream = new FileOutputStream(localFileName, false);
            } catch (FileNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            try
            {
                receivedBytes += payload.length;
                outputStream.write(payload);
                outputStream.flush();
                if (receivedBytes == expectedBytes)
                {
                    System.out.println("Received entire transfer");
                    isDone = true;
                }
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void flowReceived(CAMQPControlFlow flow)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void attachReceived(CAMQPControlAttach controlFrame)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void detachReceived(CAMQPControlDetach controlFrame)
    {
        // TODO Auto-generated method stub

    }
}
