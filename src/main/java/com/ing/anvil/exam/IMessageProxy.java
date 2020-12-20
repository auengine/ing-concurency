package com.ing.anvil.exam;

import java.util.List;

public interface IMessageProxy<T extends IMessage>
{
    void start();

    List<T> stop();

    void setMessageSender(IMessageSender<T> sender);

    void send(T message, IPriority priority);

}
