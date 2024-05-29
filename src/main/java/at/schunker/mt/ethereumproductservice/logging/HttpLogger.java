package at.schunker.mt.ethereumproductservice.logging;

import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpLogger implements HttpLoggingInterceptor.Logger {

    private static final Logger logger = LoggerFactory.getLogger(HttpLoggingInterceptor.class);

    @Override
    public void log(String message) {
        logger.info(String.format("%s", message));
    }
}
