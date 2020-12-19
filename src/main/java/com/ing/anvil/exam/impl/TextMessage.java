package com.ing.anvil.exam.impl;

import com.ing.anvil.exam.IMessage;
import com.ing.anvil.exam.IMessageProxy;
import com.ing.anvil.exam.IMessageSender;
import com.ing.anvil.exam.IPriority;

import java.util.List;

public class TextMessage implements IMessage
{
   private String text;
   public TextMessage(String text){
       this.text=text;
   }

   public String toString(){
       return text;
   }
}
