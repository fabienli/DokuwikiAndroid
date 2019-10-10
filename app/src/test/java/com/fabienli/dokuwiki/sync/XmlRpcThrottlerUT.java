package com.fabienli.dokuwiki.sync;

import org.junit.Test;

import java.util.Date;

public class XmlRpcThrottlerUT {


    /**
     * Test adding a call
     */
    @Test
    public void test_addCall(){

        XmlRpcThrottler xmlRpcThrottler = XmlRpcThrottler.resetInstance();

        xmlRpcThrottler.addCallNow();

        //System.out.println(xmlRpcThrottler.getNbCallLastMinute());
        assert(xmlRpcThrottler.getNbCallLastMinute() == 1);
    }



    /**
     * Test adding more calls than limit
     */
    @Test
    public void test_tooManyCalls(){

        XmlRpcThrottler xmlRpcThrottler = XmlRpcThrottler.resetInstance();

        xmlRpcThrottler.setLimit(100);
        int nb = 99;
        while (nb>0) {
            xmlRpcThrottler.addCallNow();
            nb -=1;
        }

        assert(xmlRpcThrottler.getNbCallLastMinute() == 99);
        assert(xmlRpcThrottler.isNextCallInLimit());

        xmlRpcThrottler.addCallNow();

        assert(xmlRpcThrottler.getNbCallLastMinute() == 100);
        assert(!xmlRpcThrottler.isNextCallInLimit());
    }

    /**
     * Test adding time to wait
     */
    @Test
    public void test_getTimeToWait(){

        XmlRpcThrottler xmlRpcThrottler = XmlRpcThrottler.resetInstance();

        xmlRpcThrottler.setLimit(9);
        int nb = 10;
        while (nb>0) {
            xmlRpcThrottler.addCallNow();
            nb -=1;
        }

        //System.out.println(xmlRpcThrottler.getTimeToWait());
        assert(xmlRpcThrottler.getTimeToWait() > 59000); // assumption that above code will last less than 1s
        assert(!xmlRpcThrottler.isNextCallInLimit());
    }


    /**
     * Test adding time to wait hardcoded exceeds limit
     */
    @Test
    public void test_getTimeToWaitHardcodedExceedLimit(){

        XmlRpcThrottler xmlRpcThrottler = XmlRpcThrottler.resetInstance();

        xmlRpcThrottler.setLimit(9);
        // add a call that happened 30s ago
        Date date30s = new Date();
        date30s.setTime(date30s.getTime() - 30000);
        //System.out.println(date30s.getTime());
        xmlRpcThrottler._callLog.add(date30s);
        // fill the backlog of calls
        int nb = 9;
        while (nb>0) {
            xmlRpcThrottler.addCallNow();
            nb -=1;
        }

        //System.out.println(xmlRpcThrottler.getTimeToWait());
        // check that we'd have to wait around 30s
        assert(xmlRpcThrottler.getTimeToWait() > 29000); // assumption that above code will last less than 1s
        assert(xmlRpcThrottler.getTimeToWait() <= 30000); // assumption that above code will last less than 1s
        assert(!xmlRpcThrottler.isNextCallInLimit());
    }

    /**
     * Test adding time to wait hardcoded within limit
     */
    @Test
    public void test_getTimeToWaitHardcodedWithinLimit(){

        XmlRpcThrottler xmlRpcThrottler = XmlRpcThrottler.resetInstance();

        xmlRpcThrottler.setLimit(10);
        // add a call that happened 61s ago
        Date date61s = new Date();
        date61s.setTime(date61s.getTime() - 61000);
        //System.out.println(date61s.getTime());
        xmlRpcThrottler._callLog.add(date61s);
        // fill the backlog of calls
        int nb = 9;
        while (nb>0) {
            xmlRpcThrottler.addCallNow();
            nb -=1;
        }

        //System.out.println(xmlRpcThrottler.getTimeToWait());
        // we should get the last item purged out, as older than 60s
        assert(xmlRpcThrottler.isNextCallInLimit());
        assert(xmlRpcThrottler.getNbCallLastMinute() == 9);
    }
}
