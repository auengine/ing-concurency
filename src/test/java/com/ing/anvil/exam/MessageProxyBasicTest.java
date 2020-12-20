package com.ing.anvil.exam;

import com.ing.anvil.exam.impl.MessageProxy;
import com.ing.anvil.exam.impl.MessageSender;
import com.ing.anvil.exam.impl.NumberPriority;
import com.ing.anvil.exam.impl.TextMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("unchecked")
public class MessageProxyBasicTest
{
    private IMessageProxy messageProxy;

    @Before
    public void setUp() throws Exception
    {
        messageProxy = new MessageProxy();
        messageProxy.setMessageSender(new MessageSender());
    }

    @Test
    public void test_start_and_stop()
    {
        messageProxy.start();
        List<IMessage> items = messageProxy.stop();
        assertTrue(items.isEmpty());
    }

    @Test
    public void test_send_and_stop()
    {
        messageProxy.send(new TextMessage("M1"), new NumberPriority(1));
        messageProxy.send(new TextMessage("M2"), new NumberPriority(1));
        messageProxy.send(new TextMessage("M3"), new NumberPriority(2));
        messageProxy.send(new TextMessage("M4"), new NumberPriority(2));
        List<IMessage> items = messageProxy.stop();
        assertTrue(items.size() == 4);
    }

    @Test
    public void test_send_start_stop() throws InterruptedException
    {
        messageProxy.send(new TextMessage("M1"), new NumberPriority(1));
        messageProxy.send(new TextMessage("M2"), new NumberPriority(1));
        messageProxy.send(new TextMessage("M3"), new NumberPriority(2));
        messageProxy.send(new TextMessage("M4"), new NumberPriority(2));
        messageProxy.start();
        TimeUnit.MILLISECONDS.sleep(1000);
        List<IMessage> items = messageProxy.stop();
        assertTrue(true);
    }

    @Test
    public void test_stop_then_start_stop() throws InterruptedException
    {
        List<IMessage> items = messageProxy.stop();
        assertTrue(items.isEmpty());
        messageProxy.send(new TextMessage("M1"), new NumberPriority(1));
        messageProxy.start();
        TimeUnit.MILLISECONDS.sleep(1000);
        items = messageProxy.stop();
        assertTrue(true);
    }

    @Test
    public void test_start_nullsend_stop() throws InterruptedException
    {
        try
        {
            messageProxy.send(new TextMessage("M1"), new NumberPriority(1));
            messageProxy.send(new TextMessage("M2"), new NumberPriority(2));
            messageProxy.send(null, new NumberPriority(2));
            messageProxy.send(null, null);
            messageProxy.send(new TextMessage("M3"), null);
            messageProxy.send(new TextMessage("M4"), null);
            LockSupport.parkNanos(1000);
            messageProxy.start();
            messageProxy.send(new TextMessage("M5"), new NumberPriority(2));
            messageProxy.send(new TextMessage("M6"), null);
            messageProxy.send(null, new NumberPriority(3));
            messageProxy.send(null, null);
            Thread.sleep(1000);
            List<IMessage> items = messageProxy.stop();

        }
        catch (Exception e)
        {
            fail();
        }
    }
}
