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
public class MessageProxyParallelCountTest
{
    private MessageProxy messageProxy ;

    @Before
    public void setUp() throws Exception {
        messageProxy = new MessageProxy();
        messageProxy.setMessageSender(new MessageSender());
    }

    @Test
    public void test_parallel_count() throws InterruptedException
    {
        int M_COUNT =1000;
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            messageProxy.send(new TextMessage("Message-P1-" + i), new NumberPriority(ThreadLocalRandom.current()
                .nextInt(1000)))

        );

        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            messageProxy.start()
        );

        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            messageProxy.send(new TextMessage("Message-P2-" + i), new NumberPriority(ThreadLocalRandom.current()
                .nextInt(1000)))
        );
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            messageProxy.start()
        );

        LockSupport.parkNanos(1000);
        List<IMessage> items= messageProxy.stop();
        messageProxy.start();
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            messageProxy.send(new TextMessage("Message-P2-" + i), new NumberPriority(ThreadLocalRandom.current()
                .nextInt(1000)))
        );
        List<IMessage> items2= messageProxy.stop();
        assertTrue( (M_COUNT*3)== (items2.size()+items.size()+messageProxy.getTotalProcessedMessageCount()));
    }
}
