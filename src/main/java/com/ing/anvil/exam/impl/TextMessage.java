package com.ing.anvil.exam.impl;

import com.ing.anvil.exam.IMessage;

public class TextMessage implements IMessage
{
   private String text;
   public TextMessage(String text){
       this.text=text;
   }
    public String toString()
    {
        return text;
    }
}
