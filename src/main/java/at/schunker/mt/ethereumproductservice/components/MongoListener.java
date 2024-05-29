package at.schunker.mt.ethereumproductservice.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

@Component
public class MongoListener extends AbstractMongoEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MongoListener.class);

    @Override
    public void onAfterSave(AfterSaveEvent event) {
        logger.info("onAfterSave({}, {})", event.getSource(), event.getDocument());
    }
}
