package at.schunker.mt.ethereumproductservice.util;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;

public class Wallet {

    public String createWallet() {
        String wallet = null;

        try {
            wallet = WalletUtils.generateNewWalletFile("Pa$$w0rd", new File("/tmp"), true);
        } catch (Exception ex) {
            System.err.println(ex);
        }

        return wallet;
    }

    public Credentials loadCredentials() {
        Credentials credentials = null;

        try {
            credentials = WalletUtils.loadCredentials("Pa$$w0rd", "/tmp");
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }

        return credentials;
    }
}
