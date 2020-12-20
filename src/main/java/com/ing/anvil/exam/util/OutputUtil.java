package com.ing.anvil.exam.util;

import java.util.List;

public class OutputUtil
{
    public static final int PRIORITY_INDEX = 2;
    public static final int ITERATION_INDEX = 1;
    public static final int START_INDEX = 0;
    private static final String seperator = "_";

    public static void logToList(List<String> totalSends, int totalCreatedThreadCount,
        int iteration, String key, String value)
    {

        totalSends.add(record(totalCreatedThreadCount, iteration, key, value));
    }

    public static String record(int totalCreatedThreadCount,
        int iteration, String priority, String value)
    {
        return totalCreatedThreadCount + seperator +
            iteration + seperator +
            priority + seperator +
            value;
    }

    public static String replace(String s, int index, String newStr
    )
    {
        String[] tokens = s.split(seperator);
        tokens[index] = newStr;
        String result = "";
        for (String token : tokens)
        {
            result += token + seperator;
        }
        return result.substring(0, result.length() - 1);

    }

    /* test purpose output checker */
    public static int checkOutPut(List<String> sendOrder)
    {
        int previousStart = 1;
        int previousIteration = 1;
        int previousPriority = Integer.MAX_VALUE;
        for (int i = 0; i < sendOrder.size(); i++)
        {
            String[] current = sendOrder.get(i).split(seperator);
            int newStart = Integer.parseInt(current[0]);
            int newIteration = Integer.parseInt(current[1]);
            int newPriority = Integer.parseInt(current[2]);
            if (newStart < previousStart)
            {
                return i;
            }
            // in same thread run
            if (newStart == previousStart)
            {
                if (newIteration < previousIteration)
                {
                    return i;
                }
                // in same thread iteration
                if (newIteration == previousIteration)
                {
                    if (previousPriority < newPriority)
                    {
                        return i;
                    }
                    previousPriority = newPriority;
                }
                else
                {
                    previousPriority = Integer.MAX_VALUE;
                }
                previousIteration = newIteration;
            }
            else
            {
                // new thread run
                previousIteration = 1;
                previousPriority = Integer.MAX_VALUE;
            }
            previousStart = newStart;
        }
        return -1;
    }
}
