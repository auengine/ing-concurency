package com.ing.anvil.exam;

import java.util.List;

public interface IMessageProxy <T extends IMessage>
{
    public void start();
    public List<T> stop();
    public void setMessageSender(IMessageSender<T> sender);
    public void send(T message, IPriority priority);

}
