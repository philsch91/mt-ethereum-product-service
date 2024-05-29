package at.schunker.mt.ethereumproductservice.dao;

//import org.web3j.crypto.CipherException;
import org.web3j.crypto.exception.CipherException;
import org.web3j.crypto.Credentials;

import java.io.IOException;

public interface CredentialsDAO {
    //public abstract boolean createWallet(Credentials credentials, String password) throws CipherException, IOException;
    public abstract boolean saveCredentials(String privateKey, String password) throws CipherException, IOException;
    public abstract Credentials loadCredentials(String address, String password);
    public abstract boolean deleteCredentials(String address, String password);
}
