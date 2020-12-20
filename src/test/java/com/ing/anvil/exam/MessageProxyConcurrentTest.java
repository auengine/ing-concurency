package com.ing.anvil.exam;

import com.ing.anvil.exam.impl.MessageProxy;
import com.ing.anvil.exam.impl.MessageSender;
import com.ing.anvil.exam.impl.NumberPriority;
import com.ing.anvil.exam.impl.TextMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("unchecked")
public class MessageProxyConcurrentTest
{
    private static final Logger logger = LoggerFactory.getLogger(MessageProxyConcurrentTest.class);
    private MessageProxy messageProxy;
    private ExecutorService executor;


    @Before
    public void setUp() throws Exception
    {
        executor = Executors.newFixedThreadPool(3);
        messageProxy = new MessageProxy();
        messageProxy.setMessageSender(new MessageSender());
    }

    @After
    public void shutDown() throws Exception
    {
        executor.shutdown();
    }

    @Test
    public void test_send_start_concurentsend_stop_check() throws InterruptedException
    {
        try
        {
            int M_COUNT_1 = 1000;
            IntStream.range(0, M_COUNT_1).parallel().forEach(i ->
                messageProxy.send(new TextMessage("Message" + i), new NumberPriority(ThreadLocalRandom.current()
                    .nextInt(1000)))

            );
            messageProxy.start();

            int T_COUNT = 100;
            int M_COUNT_2 = 1000;
            List<Future> futures = new ArrayList<>();
            final AtomicInteger startCount = new AtomicInteger(0);
            IntStream.range(0, T_COUNT).parallel().forEach(i ->
                futures.add(executor.submit(new Callable<Boolean>()
                {
                    @Override
                    public Boolean call()
                    {
                        for (int i = 0; i < M_COUNT_2; i++)
                        {
                            messageProxy.send(new TextMessage("Message" + i), new NumberPriority(ThreadLocalRandom
                                .current()
                                .nextInt(1000)));
                        }
                        return true;
                    }
                }))
            );
            logger.info("Waiting futures!");
            for (Future<?> future : futures)
            {
                while (!future.isDone())
                {
                    LockSupport.parkNanos(10);
                }
            }
            logger.info("Futures done!");

            List<IMessage> items = messageProxy.stop();
            assertTrue(messageProxy.getTotalProcessedMessageCount() + items
                .size() == (T_COUNT * M_COUNT_2 + M_COUNT_1));
        }
        catch (Exception e)
        {
            fail();
        }
    }

}
