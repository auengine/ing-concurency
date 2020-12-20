package com.ing.anvil.exam;

import com.ing.anvil.exam.impl.MessageProxy;
import com.ing.anvil.exam.impl.MessageSender;
import com.ing.anvil.exam.impl.NumberPriority;
import com.ing.anvil.exam.impl.TextMessage;
import com.ing.anvil.exam.util.OutputUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public class MessageProxyOutputTest
{
    private MessageProxy messageProxy;
    private ExecutorService executor;

    @Before
    public void setUp() throws Exception
    {
        executor = Executors.newFixedThreadPool(10);
        messageProxy = new MessageProxy();
        messageProxy.setMessageSender(new MessageSender());
    }

    @After
    public void shutDown() throws Exception
    {
        executor.shutdown();
    }
    @Test
    public void test_single_start_test() throws InterruptedException
    {
        int M_COUNT=100; int MAX_PRIORITY=100;
        int changeIndex=198;

        IntStream.range(0, M_COUNT).forEach(i ->
            {
                messageProxy.send(new TextMessage("Message" + i),  new NumberPriority(ThreadLocalRandom.current()
                    .nextInt(MAX_PRIORITY)));
            }
        );
        messageProxy.start();
        IntStream.range(0, M_COUNT).forEach(i ->
            {
                    messageProxy.send(new TextMessage("Message" + i),  new NumberPriority(ThreadLocalRandom.current()
                        .nextInt(MAX_PRIORITY)));
            }
        );
        while(messageProxy.getTotalProcessedMessageCount()!= (M_COUNT*2)){
            LockSupport.parkNanos(100);
        }
        List<IMessage> items= messageProxy.stop();
        assertTrue(items.isEmpty());
        List<String> sendOrder=messageProxy.getStatistic();
        sendOrder.set(changeIndex,
            OutputUtil.replace(sendOrder.get(changeIndex),OutputUtil.PRIORITY_INDEX
            ,""+MAX_PRIORITY+1));
        int i=OutputUtil.checkOutPut(sendOrder);
        assertTrue(i==changeIndex);

    }


    @Test
    public void test_send_start_concurentsend_stop_check() throws InterruptedException
    {
        try
        {
            int M_COUNT_1 = 10;
            IntStream.range(0, M_COUNT_1).parallel().forEach(i ->
                messageProxy.send(new TextMessage("Message" + i), new NumberPriority(ThreadLocalRandom.current()
                    .nextInt(1000)))

            );
            messageProxy.start();

            int T_COUNT = 10;
            int M_COUNT_2 = 10;
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
            Thread.sleep(1000);
            for (Future<?> future : futures)
            {
                while (!future.isDone())
                {
                    LockSupport.parkNanos(100);
                }
            }
            while (messageProxy.getTotalProcessedMessageCount()
                < (T_COUNT * M_COUNT_2 + M_COUNT_1)){
                LockSupport.parkNanos(100);
            }

            List<IMessage> items = messageProxy.stop();
            assertTrue(items.isEmpty());
            assertTrue(OutputUtil.checkOutPut(messageProxy.getStatistic())<0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

}
