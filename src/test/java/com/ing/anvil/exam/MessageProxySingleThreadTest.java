package com.ing.anvil.exam;

import com.ing.anvil.exam.impl.MessageProxy;
import com.ing.anvil.exam.impl.MessageSender;
import com.ing.anvil.exam.impl.NumberPriority;
import com.ing.anvil.exam.impl.TextMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class MessageProxySingleThreadTest
{
    private IMessageProxy messageProxy ;


    @Before
    public void setUp() throws Exception {
        messageProxy = new MessageProxy();
        messageProxy.setMessageSender(new MessageSender());
    }

    @Test
    public void test_start_send_stop() throws InterruptedException
    {
        messageProxy.start();
        IntStream.range(0, 100).forEach(i ->
            {
                messageProxy.send(new TextMessage("Message" + i), new NumberPriority(1));
            }
        );
        Thread.sleep(1000);
        List items= messageProxy.stop();
    }

    @Test
    public void test_send_start_stop() throws InterruptedException
    {
        IntStream.range(0, 100).forEach(i ->
            {
                messageProxy.send(new TextMessage("Message" + i), new NumberPriority(1));
            }
        );
        messageProxy.start();
        Thread.sleep(1000);
        List items= messageProxy.stop();
        assertTrue(items.isEmpty());
    }

    @Test
    public void test_send_start_send_stop() throws InterruptedException
    {
        int M_COUNT=100;
        IntStream.range(0, M_COUNT).forEach(i ->
            {
                messageProxy.send(new TextMessage("Message" + i), new NumberPriority(1));
            }
        );
        messageProxy.start();
        IntStream.range(0, M_COUNT).forEach(i ->
            {
                messageProxy.send(new TextMessage("Message" + i), new NumberPriority(1));
            }
        );
        Thread.sleep(1000);
        List items= messageProxy.stop();
        assertTrue(items.size()< M_COUNT *2 );
    }

    public void test_send_start_send_stop_stop() throws InterruptedException
    {
        int M_COUNT=100;
        IntStream.range(0, M_COUNT).forEach(i ->
            {
                messageProxy.send(new TextMessage("Message" + i), new NumberPriority(1));
            }
        );
        messageProxy.start();
        IntStream.range(0, M_COUNT).forEach(i ->
            {
                messageProxy.send(new TextMessage("Message" + i), new NumberPriority(1));
            }
        );
        Thread.sleep(1000);
        List items= messageProxy.stop();
        List items1= messageProxy.stop();
       assertTrue(items.size()==items1.size());
    }

}
