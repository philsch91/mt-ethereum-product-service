package at.schunker.mt.ethereumproductservice.controller;

import at.schunker.mt.ethereumproductservice.persistence.DBWalletDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.Bip44WalletUtils;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

@Controller
public class WalletController {

    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private DBWalletDAO dbWalletDAO;
    @Resource
    private File walletDestinationDirectory;

    private void logApplicationContext() {
        if (this.applicationContext == null) {
            logger.error("applicationContext is null");
            return;
        }
        logger.info("applicationContext: {}", this.applicationContext.toString());
    }

    @RequestMapping(value = "/bip39wallet")
    public ResponseEntity<String> createBip39Wallet(HttpServletResponse servletResponse) {
        this.logApplicationContext();

        File file = this.walletDestinationDirectory;
        //Bip39Wallet wallet = new Bip39Wallet("filename", "mnemonic");
        Bip39Wallet wallet = null;
        try {
            wallet = WalletUtils.generateBip39Wallet("Pa$$w0rd", file);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return null;
        }

        logger.info("Bip39Wallet filename: {}", wallet.getFilename());
        logger.info("Bip39Wallet mnemonic: {}", wallet.getMnemonic());

        servletResponse.setHeader("X-Test-Header", "bip39wallet");
        return new ResponseEntity<>(wallet.getMnemonic(), HttpStatus.OK);
    }

    @RequestMapping(value = "/bip44wallet")
    public ResponseEntity<String> createBip44Wallet(HttpServletResponse servletResponse) {
        this.logApplicationContext();

        File file = this.walletDestinationDirectory;
        //Bip39Wallet wallet = new Bip39Wallet("filename", "mnemonic");
        Bip39Wallet wallet = null;
        try {
            wallet = Bip44WalletUtils.generateBip44Wallet("Pa$$w0rd", file);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return null;
        }

        logger.info("Bip39Wallet filename: {}", wallet.getFilename());
        logger.info("Bip39Wallet mnemonic: {}", wallet.getMnemonic());

        servletResponse.setHeader("X-Test-Header", "bip44wallet");
        return new ResponseEntity<>(wallet.getMnemonic(), HttpStatus.OK);
    }
}
