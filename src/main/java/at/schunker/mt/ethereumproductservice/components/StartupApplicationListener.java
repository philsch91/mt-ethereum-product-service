package at.schunker.mt.ethereumproductservice.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StartupApplicationListener.class);
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Environment environment;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info("onApplicationEvent({})", event.toString());
        logger.info("applicationContext DisplayName: {}", this.applicationContext.getDisplayName());
        //DBWalletDAO dbWalletDAO = new DBWalletDAO();
        //AutowireCapableBeanFactory autowireCapableBeanFactory = this.applicationContext.getAutowireCapableBeanFactory();
        //autowireCapableBeanFactory.autowireBeanProperties(dbWalletDAO, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
        //autowireCapableBeanFactory.autowireBean(dbWalletDAO);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.info("onApplicationEvent({})", applicationReadyEvent.toString());
        for (String profileName : this.environment.getActiveProfiles()) {
            logger.info("active profile: {}", profileName);
        }
    }
}
