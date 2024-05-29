package at.schunker.mt.ethereumproductservice.persistence;

import at.schunker.mt.ethereumproductservice.dao.CredentialsDAO;
import at.schunker.mt.ethereumproductservice.data.WalletRepository;
import at.schunker.mt.ethereumproductservice.util.CredentialUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.crypto.*;
import org.web3j.crypto.exception.*;

import java.io.IOException;
import java.util.Optional;

@Component
public class DBWalletDAO implements CredentialsDAO {

    private static final Logger logger = LoggerFactory.getLogger(DBWalletDAO.class);
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    public void setWalletRepository(WalletRepository walletRepository) {
        logger.info("setWalletRepository");
        this.walletRepository = walletRepository;
    }

    @Override
    public boolean saveCredentials(String privateKey, String password) throws CipherException, IOException {
        if (this.walletRepository == null) {
            logger.info("walletRepository is null");
            return false;
        }

        Credentials credentials = Credentials.create(privateKey);
        String publicKey = CredentialUtils.getPublicKeyInCredentials(credentials);
        //String walletId = "04a133e2-e296-483a-ae58-19da443ea966";
        //Optional<WalletFile> optWalletFile = this.walletRepository.findById(walletId);
        String address = credentials.getAddress();
        //address = Long.toHexString(Long.parseLong(address, 16));  // NumberFormatException
        if (address.startsWith("0x")) {
            address = address.substring(2);
        }
        logger.info("createWallet {}", address);
        Optional<WalletFile> optWalletFile = this.walletRepository.findByAddress(address);

        if (optWalletFile.isPresent()) {
            logger.info("{} already saved", address);
            logger.info("{} already saved", publicKey);
            return false;
        }

        //WalletUtils.generateNewWalletFile()
        ECKeyPair keyPair = credentials.getEcKeyPair();
        WalletFile walletFile = Wallet.createStandard(password, keyPair);

        this.walletRepository.save(walletFile);
        //WalletFile.Crypto crypto = walletFile.getCrypto();
        //WalletUtils.loadJsonCredentials(password, json);
        //WalletUtils.loadCredentials(password, json);
        return true;
    }

    @Override
    public Credentials loadCredentials(String address, String password) {
        return null;
    }

    @Override
    public boolean deleteCredentials(String address, String password) {
        return false;
    }
}
