package com.ing.anvil.exam.impl;

import com.ing.anvil.exam.IMessage;
import com.ing.anvil.exam.IMessageProxy;
import com.ing.anvil.exam.IMessageSender;
import com.ing.anvil.exam.IPriority;
import com.ing.anvil.exam.util.OutputUtil;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
/* */
public class MessageProxy implements IMessageProxy<IMessage>
{
    private static final Logger logger = LoggerFactory.getLogger(MessageProxy.class);
    private static final Comparator<Pair<IPriority, IMessage>> byKey = (Pair<IPriority, IMessage> o1, Pair<IPriority, IMessage> o2) -> o2
        .getKey().compareTo(o1.getKey());
    private final LinkedBlockingQueue<Pair<IPriority, IMessage>> queue = new LinkedBlockingQueue<>();

    /*
     * Lock free implementation variables for counting and thread creation
     * Creating thread dynamically is not preferred normally, it used to increase complexity
     */
    private final AtomicReference<SenderTread> senderThread = new AtomicReference<>();
    private final AtomicInteger activeThreadCount = new AtomicInteger(0);
    private final AtomicInteger totalCreatedThreadCount = new AtomicInteger(0);
    private final AtomicReference<IMessageSender<IMessage>> sender = new AtomicReference<>();
    private final AtomicBoolean stopSending = new AtomicBoolean(false);
    private final List<String> totalSends = new ArrayList<>();
    private long totalProcessedMessageCount = 0;

    /*
     This method lock free concurrent starter of the local sender thread
     local thread number is always one.
     */
    public void start()
    {
        if (sender.get() == null)
        {
            throw new IllegalStateException("IMessage sender must be set before start!");
        }
        while (true)
        {
            /* sender thread active already */
            if (activeThreadCount.get() != 0)
            {
                break;
            }
            SenderTread current = senderThread.get();
            SenderTread candidate = new SenderTread(SenderTread.class.getSimpleName() + totalCreatedThreadCount.get());
            if (senderThread.compareAndSet(current, candidate))
            {
                senderThread.get().setDaemon(true);
                stopSending.set(false);
                activeThreadCount.incrementAndGet();
                totalCreatedThreadCount.incrementAndGet();
                senderThread.get().start();
                break;
            }
            else
            {
                LockSupport.parkNanos(1);
            }
        }
    }

    /*
   This method lock free concurrent stopper of the local sender thread
   local thread number is always one.Start and stop can be done many times.
   */
    public List<IMessage> stop()
    {
        try
        {
           boolean interuptSended=false;
            while (true)
            {
                /* no thread to destroy */
                if (activeThreadCount.get() == 0)
                {
                    break;
                }
                SenderTread current = senderThread.get();
                // try break sending and interrupt sender thread
                if (senderThread.compareAndSet(current, null))
                {
                    stopSending.set(true);
                    current.interrupt();
                    current.join();
                    interuptSended=true;
                    break;
                }
                else
                {
                    LockSupport.parkNanos(1);
                }
            }
            List<Pair<IPriority, IMessage>> unSend = new ArrayList<>();
            queue.drainTo(unSend);
            List<IMessage> result= unSend.stream().map(Pair::getValue).
                                       collect(Collectors.toList());
            if(interuptSended){
                activeThreadCount.decrementAndGet();
            }
            return result;

        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException("Cannot join sender thread!");
        }

    }

    public void setMessageSender(final IMessageSender<IMessage> sender)
    {
        this.sender.set(sender);
    }

    public void send(final IMessage message, final IPriority priority)
    {
        if (message == null || priority == null)
        {
            return;
        }
        // no backpressure -> for backpressure use put method
        queue.add(new Pair<>(priority, message));


    }
    /* Test purpose */
    public int getActiveThreadCount()
    {
        return activeThreadCount.get();
    }

    public int getTotalCreatedThreadCount()
    {
        return totalCreatedThreadCount.get();
    }

    public long getTotalProcessedMessageCount()
    {
        return totalProcessedMessageCount;
    }

    public List<String> getStatistic()
    {
        return totalSends;
    }

    private class SenderTread extends Thread
    {
        private final String name;
        private long localMessageCount = 0;
        private long errorCount = 0;
        final List<Pair<IPriority, IMessage>> sendList = new ArrayList<>();

        private SenderTread(String name)
        {
            this.name = name;
        }

        public void run()
        {
            Pair<IPriority, IMessage> item ;

            logger.info("Starting sender thread {}", name);
            int iteration = 0;
            try
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    //handle interrupt case if take and interrupt occur simultaneously
                    item = queue.take();
                    iteration++;
                    logger.info("Items are available!");
                    queue.drainTo(sendList);
                    sendList.add(item);
                    sendList.sort(byKey);
                    logger.info("Sending ordered messages!");
                    Iterator<Pair<IPriority, IMessage>> iterator = sendList.iterator();
                    while (iterator.hasNext() && !stopSending.get())
                    {
                        item = iterator.next();
                        logger.info("Sending message priority: {} , message: {}", item.getKey().toString(), item
                            .getValue().toString());
                        try
                        {
                            localMessageCount++;
                            sender.get().send(item.getValue());
                            totalProcessedMessageCount++;
                        }
                        catch (IOException e)
                        {
                            queue.add(item);
                            errorCount++;
                            logger.warn("Could not send message, try later!");
                        }
                        /* output test purpose */
                        OutputUtil.logToList(totalSends, totalCreatedThreadCount.get(), iteration, item.getKey()
                                .toString(),
                            item.getValue().toString());
                        iterator.remove();
                    }
                }
            }
            catch (InterruptedException e)
            {
                logger.info("Sender thread is interrupted!");

            }
            catch (Exception e)
            {
                logger.error("Unhandled exception in {} ", name, e);
            }
            finally
            {
                queue.addAll(sendList);
                logger
                    .warn("Total work done in that sender thread {} count: {} error: {}", name, localMessageCount, errorCount);
            }
        }
    }
}
