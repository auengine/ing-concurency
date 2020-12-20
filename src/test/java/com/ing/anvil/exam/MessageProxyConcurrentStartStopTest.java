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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class MessageProxyConcurrentStartStopTest
{
    private static final Logger logger = LoggerFactory.getLogger(MessageProxyConcurrentStartStopTest.class);
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
    public void test_concurent_start() throws InterruptedException
    {
        int M_COUNT = 100;
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            executor.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    messageProxy.start();
                }
            })
        );
        while (messageProxy.getActiveThreadCount()==0){
            LockSupport.parkNanos(10);
        }
        Thread.sleep(100);
        assertTrue(messageProxy.getActiveThreadCount()==1);
    }

    @Test
    public void test_concurent_stop()
    {
        int M_COUNT = 1000;
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            executor.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    messageProxy.stop();
                }
            })
        );
        assertTrue(messageProxy.getActiveThreadCount()==0);
        assertTrue(messageProxy.getTotalCreatedThreadCount()==0);
    }

    @Test
    public void test_concurent_start_and_stop()
    {
        int M_COUNT = 100;
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            executor.submit(new Runnable()
            {
                @Override
                public void run()
                {

                    if (ThreadLocalRandom.current().nextBoolean())
                    {
                        messageProxy.start();
                    }
                    else
                    {
                        messageProxy.stop();
                    }

                }
            })
        );

    }

    @Test
    public void test_concurent_start_send_stop() throws InterruptedException
    {
        int M_COUNT = 10;
        List<Future> futures = new ArrayList<>();
        final AtomicInteger startCount = new AtomicInteger(0);
        IntStream.range(0, M_COUNT).parallel().forEach(i ->
            futures.add(executor.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int i = 0; i < M_COUNT; i++)
                    {
                        if (ThreadLocalRandom.current().nextBoolean())
                        {
                            messageProxy.start();
                            startCount.incrementAndGet();
                        }
                        else
                        {
                            messageProxy.stop();
                        }
                        messageProxy.send(new TextMessage("Message" + i), new NumberPriority(ThreadLocalRandom.current()
                            .nextInt(1000)));

                    }
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
        assertTrue(messageProxy.getActiveThreadCount() < 2);
        assertTrue(messageProxy.getTotalCreatedThreadCount() <= startCount.get());

    }

}
