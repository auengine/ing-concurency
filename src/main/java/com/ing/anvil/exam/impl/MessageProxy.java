package com.ing.anvil.exam.impl;

import com.ing.anvil.exam.IMessage;
import com.ing.anvil.exam.IMessageProxy;
import com.ing.anvil.exam.IMessageSender;
import com.ing.anvil.exam.IPriority;
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



/* Java program to find a Pair which has maximum score*/

public class MessageProxy implements IMessageProxy
{
    private static final Logger logger = LoggerFactory.getLogger(MessageProxy.class);
    private static final Comparator<Pair<IPriority, IMessage>> byKey = (Pair<IPriority, IMessage> o1, Pair<IPriority, IMessage> o2) -> o1
        .getKey().compareTo(o2.getKey());
    //thread safe blocking queue concurrentlinkedqueue better in performance but without lock
    private final LinkedBlockingQueue<Pair<IPriority, IMessage>> queue = new LinkedBlockingQueue<>();
    /* single sender thread use list no race here */
    private final List<Pair<IPriority, IMessage>> sendList = new ArrayList<>();
    private final AtomicReference<SenderTread> senderThread = new AtomicReference<>();
    /* count of sender thread 1 is fixed for current impl */
    private final AtomicInteger activeThreadCount = new AtomicInteger(0);
    /* debug purpose counter */
    private static final AtomicInteger totalCreatedThreadCount = new AtomicInteger(0);
    //no sync required
    private final AtomicReference<IMessageSender> sender = new AtomicReference<>();
    //to stop sending operation and perform check interupt
    private final AtomicBoolean stopSending = new AtomicBoolean(false);

    /*
     This method lock free concurrent starter of the local sender thread
     local thread number is always one.
     */
    public void start()
    {

        while (true)
        {
            /* sender thread active already */
            if (activeThreadCount.get() != 0)
            {
                break;
            }
            SenderTread current= senderThread.get();
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
    public List stop()
    {
        try
        {
            while (true)
            {
                /* no thread to destroy */
                if (activeThreadCount.get() == 0)
                {
                    break;
                }
                SenderTread current = senderThread.get();
                // try break sending and interupt sender thread
                if (senderThread.compareAndSet(current, null))
                {
                    stopSending.set(true);
                    current.interrupt();
                    current.join();
                    activeThreadCount.decrementAndGet();
                    break;
                }
                else
                {
                    LockSupport.parkNanos(1);
                }
            }

            List<Pair<IPriority, IMessage>> unSend = new ArrayList<>();
            queue.drainTo(unSend);
            unSend.addAll(sendList);
            sendList.clear();
            return unSend;
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException("Cannot join sender thread!");
        }

    }

    public void setMessageSender(final IMessageSender sender)
    {
        this.sender.set(sender);
    }

    public void send(final IMessage message, final IPriority priority)
    {
        queue.add(new Pair<>(priority, message));
    }


    private class SenderTread extends Thread
    {
        private long count = 0;
        private long errorCount = 0;
        private final String name;

        private SenderTread(String name)
        {
            this.name = name;
        }

        public void run()
        {
            Pair<IPriority, IMessage> item = null;
            logger.info("Starting sender thread {}", name);
            try
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    //handle interrupt case if take and interrupt occur simultaniously
                    item = queue.take();
                    logger.info("Items are available!");
                    queue.drainTo(sendList);
                    sendList.add(item);
                    Collections.sort(sendList, byKey);
                    logger.info("Sending ordered messages!");
                    Iterator<Pair<IPriority, IMessage>> iterator = sendList.iterator();
                    while (iterator.hasNext() && !stopSending.get())
                    {
                        item = iterator.next();
                        logger.info("Sending message priotirity: {} , message: {}", item.getKey().toString(), item
                            .getValue().toString());
                        try
                        {
                            count++;
                            sender.get().send(item.getValue());
                        }
                        catch (IOException e)
                        {
                            errorCount++;
                            logger.warn("Could not send message,skipping it!");
                        }
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
                logger.warn("Total work done in that sender thread {} count: {} error: {}", name,count, errorCount);
            }
        }
    }

    /* Test purpose */
    public int getActiveThreadCount(){
         return activeThreadCount.get();
    }

    public int getTotalCreatedThreadCount(){
        return totalCreatedThreadCount.get();
    }
}
