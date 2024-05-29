package at.schunker.mt.ethereumproductservice.persistence;

import at.schunker.mt.ethereumproductservice.dao.Bip39CredentialsDAO;
import at.schunker.mt.ethereumproductservice.dao.CredentialsDAO;
import at.schunker.mt.ethereumproductservice.util.CredentialUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.*;
import org.web3j.crypto.exception.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class FileWalletDAO implements CredentialsDAO, Bip39CredentialsDAO {

    private static final Logger logger = LoggerFactory.getLogger(FileWalletDAO.class);

    private String directoryPath;

    public FileWalletDAO(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    private String generateWallet(String password) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, CipherException, IOException {
        String walletFileName = WalletUtils.generateNewWalletFile(password, new File(this.directoryPath));
        return walletFileName;
    }

    private String generateWallet(String password, String privateKey) throws CipherException, IOException {
        ECKeyPair keyPair = CredentialUtils.createKeyPair(privateKey);
        return this.generateWallet(password, keyPair, false);
    }

    private String generateWallet(String password, ECKeyPair keyPair, boolean bip44) throws CipherException, IOException {
        String walletFileName = WalletUtils.generateWalletFile(password, keyPair, new File(this.directoryPath), false);
        return walletFileName;
    }

    private Bip39Wallet generateBip39Wallet(String password) throws CipherException, IOException {
        Bip39Wallet bip39Wallet = WalletUtils.generateBip39Wallet(password, new File(this.directoryPath));
        //String mnemonic = bip39Wallet.getMnemonic();
        //String filename = bip39Wallet.getFilename();
        return bip39Wallet;
    }

    /**
     * Generates a BIP39 wallet object for
     * BIP44 compatible multi-account hierarchical deterministic wallets.
     * @param password
     * @return
     * @throws CipherException
     * @throws IOException
     */
    public Bip39Wallet generateBip44Wallet(String password) throws CipherException, IOException {
        Bip39Wallet bip39Wallet = Bip44WalletUtils.generateBip44Wallet(password, new File(this.directoryPath));
        return bip39Wallet;
    }

    /**
     * Creates an initial BIP39 mnemonic.
     * @param password
     * @return
     * @throws CipherException
     * @throws IOException
     */
    public String generateMnemonic(String password) throws CipherException, IOException {
        Bip39Wallet bip39Wallet = this.generateBip44Wallet(password);
        String mnemonic = bip39Wallet.getMnemonic();
        return mnemonic;
    }

    /**
     * Example path:  // m/44'/60'/0'/0/0
     * Example path parameter: final int[] path = {44 | HARDENED_BIT, 60 | HARDENED_BIT, 0 | HARDENED_BIT, 0, 0};
     * @param password
     * @param path
     * @return
     * @throws CipherException
     * @throws IOException
     */
    private String getAddress(String mnemonic, String password, int[] path) throws CipherException, IOException {
        Credentials credentials = CredentialUtils.createCredentials(mnemonic, password, path);
        String address = credentials.getAddress();
        return address;
    }

    /** Bip39CredentialsDAO */

    public boolean saveBip39Credentials(String mnemonic, String password) throws CipherException, IOException {
        Bip32ECKeyPair masterKeyPair = CredentialUtils.generateBip32MasterKeyPair(mnemonic, password);
        Credentials masterCredentials = Credentials.create(masterKeyPair);
        //return this.createWallet(masterCredentials, password);
        BigInteger privateKey = masterCredentials.getEcKeyPair().getPrivateKey();
        String privateKeyInHex = privateKey.toString(16);
        return this.saveCredentials(privateKeyInHex, password);
    }

    public Credentials loadBip39Credentials(String mnemonic, String password) {
        Bip32ECKeyPair masterKeyPair = CredentialUtils.generateBip32MasterKeyPair(mnemonic, password);
        Credentials masterCredentials = Credentials.create(masterKeyPair);
        String address = masterCredentials.getAddress();
        Credentials credentials = this.loadCredentials(address, password);
        return credentials;
    }
    public boolean deleteBip39Credentials(String mnemonic, String password) {
        return false;
    }

    /** CredentialsDAO */

    @Override
    public boolean saveCredentials(String privateKey, String password) throws CipherException, IOException {
        Credentials credentials = Credentials.create(privateKey);
        String publicKey = CredentialUtils.getPublicKeyInCredentials(credentials);

        String fileName = this.directoryPath + "/" + publicKey + ".json";
        File newWalletFile = new File(fileName);

        logger.info("newWalletFile.getName(): " + newWalletFile.getName());
        logger.info("newWalletFile.getPath(): " + newWalletFile.getAbsolutePath());

        if (newWalletFile.exists()) {
            return false;
        }

        ECKeyPair keyPair = credentials.getEcKeyPair();
        String walletFileName = WalletUtils.generateWalletFile(password, keyPair, new File(this.directoryPath), false);
        walletFileName = this.directoryPath + "/" + walletFileName;
        logger.info("walletFileName: " + walletFileName);

        Path walletPath = Paths.get(walletFileName);
        logger.info("walletPath: " + walletPath.toString());
        //Path walletPathFileName = walletPath.getFileName();

        //File walletFile = walletPath.toFile();
        //boolean res = walletFile.renameTo(newWalletFile);
        //logger.info("res: " + res);

        //String pubKeyFileName = publicKey + ".json";
        String pubKeyFileName = newWalletFile.getName();
        logger.info("pubKeyFileName: " + pubKeyFileName);
        //Files.move(walletPath, walletPath.resolveSibling(pubKeyFileName));

        // rename wallet file from temp name to public key
        Path newWalletPath = Files.move(walletPath, newWalletFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        //Path newWalletPath = Files.copy(walletPath, newWalletFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        logger.info("newWalletPath: " + newWalletPath.toString());

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
