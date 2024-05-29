package at.schunker.mt.ethereumproductservice;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestListener extends RunListener {

    Logger logger = LogManager.getLogger(TestListener.class);

    @Override
    public void testRunStarted(Description description) throws Exception {
        // Called before any tests have been run
        logger.info("testRunStarted");
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        // Called when all tests have finished
        logger.info("testRunFinished");
    }
}
