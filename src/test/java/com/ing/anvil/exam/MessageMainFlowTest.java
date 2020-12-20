package com.ing.anvil.exam;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

@SuppressWarnings("unchecked")
public class MessageMainFlowTest
{
    @Before
    public void setUp()
    {

    }

    @Test
    public void test_main_flow()
    {
        try
        {
           // new MainFlow().test();
        }
        catch (Exception e)
        {
            fail();
        }

    }
}
