package at.schunker.mt.ethereumproductservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class WalletControllerConfig {

    @Value("${walletDirectoryPath}")
    private String walletDirectoryPath;

    @Bean
    //@Bean(name = "walletDestinationDirectory")
    public File walletDestinationDirectory() {
        File file = new File(this.walletDirectoryPath);
        return file;
    }
}
