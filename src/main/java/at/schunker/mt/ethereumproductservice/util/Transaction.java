package at.schunker.mt.ethereumproductservice.util;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;

public class Transaction {

    public static String createTransactionHexString(Credentials credentials, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, BigInteger value, String init) {
        RawTransaction rawTransaction = RawTransaction.createContractTransaction(nonce, gasPrice, gasLimit, value, init);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        return hexValue;
    }

    public static EthGetTransactionCount getTransactionCount(Web3j web3j, String address) {
        EthGetTransactionCount result = new EthGetTransactionCount();
        try {
            web3j.ethGetTransactionCount(address,
                    DefaultBlockParameter.valueOf("latest")).send();
        } catch (IOException ioex) {
            System.err.println(ioex.getMessage());
        }
        return result;
    }
}
