package com.ing.anvil.exam.impl;

import com.ing.anvil.exam.IMessage;
import com.ing.anvil.exam.IMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

public class MessageSender implements IMessageSender<IMessage>
{
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    public void send(final IMessage message) throws IOException
    {
        if ((int)((Math.random() * 1000) % 101) == 0)
        {
            logger.info("Network broken cannot sent the message!");
            throw new IOException("Network error!");
        }
        logger.info("Message sent!");
        LockSupport.parkNanos(100);
    }
}
