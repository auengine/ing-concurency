package com.ing.anvil.exam.impl;

import com.ing.anvil.exam.IPriority;

import java.util.Random;

public class NumberPriority implements IPriority
{
    private final Integer priority;

    public NumberPriority(Integer priority)
    {
        this.priority = priority;
    }

    public int compareTo(IPriority o)
    {
        NumberPriority s = (NumberPriority) o;
       return this.priority == s.getPriority() ? 0 :this.priority>s.getPriority() ? 1 : -1;
    }

    public Integer getPriority()
    {
        return priority;
    }
    public String toString(){
        return priority.toString();
    }
}
