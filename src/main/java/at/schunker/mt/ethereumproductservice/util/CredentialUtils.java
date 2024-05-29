package at.schunker.mt.ethereumproductservice.util;

import org.web3j.crypto.*;
import static org.web3j.crypto.Bip32ECKeyPair.HARDENED_BIT;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class CredentialUtils {

    public static ECKeyPair createKeyPair(String privateKey) {
        BigInteger privateKeyBigInt = new BigInteger(privateKey, 16);
        ECKeyPair keyPair = ECKeyPair.create(privateKeyBigInt);
        return keyPair;
    }

    /**
     * Generates a generic BIP32 master key pair.
     * @param mnemonic
     * @param password
     * @return
     */
    public static Bip32ECKeyPair generateBip32MasterKeyPair(String mnemonic, String password) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
        Bip32ECKeyPair masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed);
        return masterKeyPair;
    }

    /**
     * Generates a BIP44 key pair for Ethereum
     * derived from a BIP32 master key
     * and a respective BIP44 derivation path.
     * Wrapper around Bip44WalletUtils.generateBip44KeyPair()
     * BIP44 derivation path: m/purpose'/coin_type'/account'/chain/address_index
     * BIP44 Ethereum mainnet path: m/44'/60'/0'/0/0
     * @param mnemonic
     * @param password
     * @param testNet
     * @return
     */
    public static Bip32ECKeyPair generateBip44KeyPair(String mnemonic, String password, boolean testNet) {
        Bip32ECKeyPair masterKeyPair = CredentialUtils.generateBip32MasterKeyPair(mnemonic, password);
        Bip32ECKeyPair bip32KeyPair = Bip44WalletUtils.generateBip44KeyPair(masterKeyPair, testNet);
        return bip32KeyPair;
    }

    /**
     * Generates a BIP32 or BIP44 key pair
     * derived from a BIP32 master key
     * and a BIP32 or BIP44 derivation path.
     * BIP32 derivation path: m/0'/0'/k' with k = extended private key
     * BIP32 Ethereum mainnet path: m/44'/60'/0'/0
     * BIP32 Ethereum mainnet path parameter: int[] path = {44 | HARDENED_BIT, 60 | HARDENED_BIT, 0 | HARDENED_BIT, 0};
     *
     * BIP44 derivation path: m/purpose'/coin_type'/account'/chain/address_index
     * BIP44 Ethereum mainnet path: m/44'/60'/0'/0/0
     * BIP44 Ethereum testnet path: m/44'/1'/0'/0
     * BIP44 Ethereum testnet path parameter: int[] path = {44 | HARDENED_BIT, 1 | HARDENED_BIT, 0 | HARDENED_BIT, 0};
     * Credentials credentials = Credentials.create(bip32KeyPair)
     * @param mnemonic
     * @param password
     * @param path
     * @return
     */
    public static Bip32ECKeyPair generateBip32KeyPair(String mnemonic, String password, int[] path) {
        Bip32ECKeyPair masterKeyPair = CredentialUtils.generateBip32MasterKeyPair(mnemonic, password);
        /*
        if (masterKeyPair.getChildNumber() == 0) {
            return masterKeyPair;
        } */
        return Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path);
    }

    /**
     * int[] path = {44 | HARDENED_BIT, 60 | HARDENED_BIT, 0 | HARDENED_BIT, 0, 0};
     * @param mnemonic
     * @param password
     * @return
     */
    public static Credentials createCredentials(String mnemonic, String password, int[] path) {
        Bip32ECKeyPair bip32KeyPair = CredentialUtils.generateBip32KeyPair(mnemonic, password, path);
        Credentials credentials = Credentials.create(bip32KeyPair);
        return credentials;
    }

    public static Credentials createCredentials() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        ECKeyPair keyPair = Keys.createEcKeyPair();
        BigInteger privateKeyBigInt = keyPair.getPrivateKey();
        String privateKeyInHex = privateKeyBigInt.toString(16);
        //Credentials.create(keyPair);
        return Credentials.create(privateKeyInHex);
    }

    /**
     * Deprecated: Use Credentials.create(String privateKey) instead
     * @param privateKey
     * @return Credentials
     */
    @Deprecated
    public static Credentials createCredentials(String privateKey) {
        BigInteger privateKeyBigInt = new BigInteger(privateKey, 16);
        ECKeyPair keyPair = ECKeyPair.create(privateKeyBigInt);
        return Credentials.create(keyPair);
    }

    public static String getPublicKeyInHex(String privateKeyInHex) {
        BigInteger privateKey = new BigInteger(privateKeyInHex, 16);
        ECKeyPair keyPair = ECKeyPair.create(privateKey);
        BigInteger publicKey = keyPair.getPublicKey();
        String publicKeyInHex = publicKey.toString(16);
        return publicKeyInHex;
    }

    public static String getPublicKeyInCredentials(Credentials credentials) {
        ECKeyPair keyPair = credentials.getEcKeyPair();
        String publicKey = keyPair.getPublicKey().toString(16);
        return publicKey;
    }

    public static String getPrivateKeyInCredentials(Credentials credentials) {
        ECKeyPair keyPair = credentials.getEcKeyPair();
        String privateKey = keyPair.getPrivateKey().toString(16);
        return privateKey;
    }

    public static String getAddressInCredentials(Credentials credentials) {
        return credentials.getAddress();
    }
}
