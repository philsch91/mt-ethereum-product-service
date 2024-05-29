package at.schunker.mt.ethereumproductservice.dao;

//import org.web3j.crypto.CipherException;
import org.web3j.crypto.exception.CipherException;
import org.web3j.crypto.Credentials;

import java.io.IOException;

public interface Bip39CredentialsDAO extends CredentialsDAO {
    public abstract boolean saveBip39Credentials(String mnemonic, String password) throws CipherException, IOException;
    public abstract Credentials loadBip39Credentials(String mnemonic, String password);
    public abstract boolean deleteBip39Credentials(String mnemonic, String password);
}
