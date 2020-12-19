package com.ing.anvil.exam;

import com.ing.anvil.exam.impl.MessageProxy;
import com.ing.anvil.exam.impl.MessageSender;
import com.ing.anvil.exam.impl.NumberPriority;
import com.ing.anvil.exam.impl.TextMessage;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unchecked")
public class MainFlow
{
    private static final Logger logger = LoggerFactory.getLogger(MainFlow.class);

    public static void main(String[] args) throws InterruptedException
    {
        new MainFlow().test();
    }

    public void test()
    {
        IMessageSender messageSender = new MessageSender();
        IMessageProxy messageProxy = new MessageProxy();
        messageProxy.setMessageSender(messageSender);
        int threadCount = 10;
        //ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try
        {
            //send a few before start
            for (int i = 0; i < 10; i++)
            {
                messageProxy.send(new TextMessage(Thread.currentThread()
                    .getName() + ":Message" + i), new NumberPriority(ThreadLocalRandom.current()
                    .nextInt(100)));
            }
            //create threads
            List<Thread> threadList = new ArrayList<Thread>();
            for (int i = 0; i < threadCount; i++)
            {
                ProducerThread thread = new ProducerThread(messageProxy, 50000);
                thread.setName("Thread" + i);
                threadList.add(thread);
            }
            //start buffer
            messageProxy.start();
            //start threads
            for (int i = 0; i < threadCount; i++)
            {
                threadList.get(i).start();
            }
            //wait threads
            for (int i = 0; i < threadCount; i++)
            {
                threadList.get(i).join();
            }
            List<Pair<IPriority, IMessage>> unsendPairs = messageProxy.stop();
            //List<Pair<IPriority,IMessage>> unsendPairs = (List<Pair<IPriority,IMessage>>)unsend;
            logger.info("Number of unsend message is {}", unsendPairs);
            unsendPairs.stream().forEach(p ->
                logger.info("Unsend details priority: {} message: {}",
                    p.getKey().toString(),
                    p.getValue().toString())
            );
        }
        catch (InterruptedException e)
        {
            logger.error("Interupted", e);
        }

    }

    public class ProducerThread extends Thread
    {
        private final IMessageProxy messageProxy;
        private final int sendCount;

        public ProducerThread(IMessageProxy messageProxy, int sendCount)
        {
            this.messageProxy = messageProxy;
            this.sendCount = sendCount;
        }

        public void run()
        {
            try
            {
                for (int i = 0; i < sendCount; i++)
                {
                    messageProxy.send(new TextMessage(Thread.currentThread()
                        .getName() + ":Message" + i), new NumberPriority(ThreadLocalRandom.current()
                        .nextInt(100)));
                    Thread.sleep(ThreadLocalRandom.current().nextInt(2));
                }
                System.out.println(Thread.currentThread().getName() + " is finished!");
            }
            catch (InterruptedException e)
            {
                logger.error("Interupted!", e);
            }
        }
    }
}
