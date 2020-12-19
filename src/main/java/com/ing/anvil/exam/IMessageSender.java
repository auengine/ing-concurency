package com.ing.anvil.exam;

import java.io.IOException;

public interface IMessageSender<T extends IMessage>
{
        public void send(T message) throws IOException;

}
