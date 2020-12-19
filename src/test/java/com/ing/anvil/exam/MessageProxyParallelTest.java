package com.ing.anvil.exam;

import com.ing.anvil.exam.impl.MessageProxy;
import com.ing.anvil.exam.impl.MessageSender;
import com.ing.anvil.exam.impl.NumberPriority;
import com.ing.anvil.exam.impl.TextMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class MessageProxyParallelTest
{
    private IMessageProxy messageProxy ;

    @Before
    public void setUp() throws Exception {
        messageProxy = new MessageProxy();
        messageProxy.setMessageSender(new MessageSender());
    }

    @Test
    public void test_parallel_send_and_stop() throws InterruptedException
    {
        int M_COUNT =100000;
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            messageProxy.send(new TextMessage("Message" + i), new NumberPriority(ThreadLocalRandom.current()
                .nextInt(1000)))

        );
        List items= messageProxy.stop();
        assertTrue(items.size()==M_COUNT);
    }



    @Test
    public void test_parallel_send_start_send_stop_send_stop() throws InterruptedException
    {
        int M_COUNT =1000;
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            messageProxy.send(new TextMessage("Message-P1-" + i), new NumberPriority(ThreadLocalRandom.current()
                .nextInt(1000)))

        );
        messageProxy.start();
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            messageProxy.send(new TextMessage("Message-P2-" + i), new NumberPriority(ThreadLocalRandom.current()
                .nextInt(1000)))

        );
        LockSupport.parkNanos(1000);
        List items= messageProxy.stop();
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            messageProxy.send(new TextMessage("Message-P3-" + i), new NumberPriority(ThreadLocalRandom.current()
                .nextInt(1000)))
        );
        List items2= messageProxy.stop();
        assertTrue( M_COUNT== items2.size());
    }
}
