package at.schunker.mt.ethereumproductservice;


import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
//@Configuration
//@EnableAutoConfiguration
//@ComponentScan
public class ProductServiceApplication implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceApplication.class);
    //private static final Logger logger = LogManager.getLogger(ProductServiceApplication.class);

    @Autowired
    private ApplicationContext applicationContext;
    private ConfigurableEnvironment environment;

    public static void main( String[] args ) {
        SpringApplication.run(ProductServiceApplication.class, args);
        System.out.println( "SpringApplication started" );
    }

    public ProductServiceApplication(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("environment: {}", this.environment.toString());
        List<String> profiles = Arrays.asList("prod", "dev");
        boolean validProfile = false;

        for (String profile : profiles) {
            for (String activeProfile : this.environment.getActiveProfiles()) {
                if (activeProfile.equals(profile)) {
                    validProfile = true;
                }
            }
        }

        if (!validProfile) {
            throw new RuntimeException("invalid profile");
        }
    }
}
