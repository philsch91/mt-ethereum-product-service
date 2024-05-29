package at.schunker.mt.ethereumproductservice.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Bip44WalletUtils;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.utils.Numeric;

import static org.web3j.crypto.Bip32ECKeyPair.HARDENED_BIT;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
class CredentialUtilsTest {

    Logger logger = LogManager.getLogger(CredentialUtilsTest.class);

    private String mnemonic = "symptom theory real work begin possible digital curtain medal weapon wish tag";
    private String password = null;
    private String expectedSeed = "0x8c8d947b352cbce2c933e9980ce80dfdd11d99423b5b737d95d9bbfb146552233389b0c0bf1f53e8a9eba9bf05f5a0841776c9c345dddb623ca17ea2b81a9799";
    private String expectedRootKey = "xprv9s21ZrQH143K2BLr3v6PoqyTdcS2oZNBkrfLKBM3TjGENhJ1itqakM8LYVjdwsP9MPnYUcs1AM3CWSrhYWXqPachdZAYraXp4DKwGw6aK8J";
    private String expectedExtendedPrivateKey = "xprv9zaHjJs9Mvmm3yUd2fbRVn4ymVauVwyHZtPAQXzaHL98z8CYcBnqoTkcwvMhYEquXBjHZdci3maJ7rQ5oHwxBbgyhCpcmhp49E1ks21p3Xt";
    private String expectedExtendedPublicKey = "xpub6DZe8pQ3CJL4GTZ68h8Rrv1iKXRPuQh8w7JmCvQBqfg7rvXh9j76MG56oCgssQAvCLEC7m7owpSL69EW55SY7eAD2bPouqnr6PoWTW84gmD";

    @Test
    public void testBip39Seed() {
        String methodName = ReflectionHelper.getMethodName(CredentialUtilsTest.class);
        logger.info("==== {} ====", methodName);
        byte[] rawSeed = MnemonicUtils.generateSeed(this.mnemonic, this.password);
        //String actualSeed = new String(rawSeed, StandardCharsets.UTF_8);
        //String actualSeed = new String(rawSeed);
        String actualSeed = Numeric.toHexString(rawSeed);
        logger.info("{} seed: {}", methodName, actualSeed);
        assertEquals(this.expectedSeed, actualSeed);
    }

    @Test
    public void testBip32RootKey() {
        String methodName = ReflectionHelper.getMethodName(CredentialUtilsTest.class);
        logger.info("==== {} ====", methodName);
        /*
        Bip32ECKeyPair rootKeyPair = CredentialUtils.generateBip32MasterKeyPair(this.mnemonic, this.password);
        BigInteger rootPrivateKeyBigInt = rootKeyPair.getPrivateKey();
        String rootPrivateKeyInHex = rootPrivateKeyBigInt.toString(16);
        logger.info("{} root private key: {}", methodName, rootPrivateKeyInHex);
        //assertEquals(this.rootKey, rootPrivateKeyInHex);
        */

        Bip32ECKeyPair masterKeyPair = CredentialUtils.generateBip32MasterKeyPair(this.mnemonic, this.password);
        String bip32RootKey = Base58.encode(Bip32Utils.addChecksum(Bip32Utils.serializePrivate(masterKeyPair)));
        logger.info("{} BIP32 root key: {}", methodName, bip32RootKey);
        assertEquals(this.expectedRootKey, bip32RootKey);
    }

    @Test
    public void testBip32ExtendedKeys() {
        String methodName = ReflectionHelper.getMethodName(CredentialUtilsTest.class);
        logger.info("==== {} ====", methodName);

        //Bip32ECKeyPair bip44KeyPair = CredentialUtils.generateBip44KeyPair(this.mnemonic, this.password, false);
        //String extendedPrivateKey = Base58.encode(Bip32Utils.addChecksum(Bip32Utils.serializePrivate(bip44KeyPair)));

        int[] path = {44 | HARDENED_BIT, 60 | HARDENED_BIT, 0 | HARDENED_BIT, 0};
        Bip32ECKeyPair bip32KeyPair = CredentialUtils.generateBip32KeyPair(this.mnemonic, this.password, path);
        // BIP32 extended private key
        String extendedPrivateKey = Base58.encode(Bip32Utils.addChecksum(Bip32Utils.serializePrivate(bip32KeyPair)));
        logger.info("{} BIP32 extended private key: {}", methodName, extendedPrivateKey);
        assertEquals(this.expectedExtendedPrivateKey, extendedPrivateKey);
        // BIP32 extended public key
        String extendedPublicKey = Base58.encode(Bip32Utils.addChecksum(Bip32Utils.serializePublic(bip32KeyPair)));
        logger.info("{} BIP32 extended public key: {}", methodName, extendedPublicKey);
        assertEquals(this.expectedExtendedPublicKey, extendedPublicKey);
    }
    /*
    @Test
    public void testBip44ExtendedKeys() {
        String methodName = ReflectionHelper.getMethodName(CredentialUtilsTest.class);
        logger.info("==== {} ====", methodName);

        Bip32ECKeyPair bip44KeyPair = CredentialUtils.generateBip44KeyPair(this.mnemonic, this.password, false);
        String extendedPrivateKey = Base58.encode(Bip32Utils.addChecksum(Bip32Utils.serializePrivate(bip44KeyPair)));
        logger.info("{} extended private key: {}", methodName, extendedPrivateKey);
        assertEquals(this.expectedExtendedPrivateKey, extendedPrivateKey);
    } */

}