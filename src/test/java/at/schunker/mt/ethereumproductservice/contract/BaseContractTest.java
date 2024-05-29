package at.schunker.mt.ethereumproductservice.contract;

import at.schunker.mt.ethereumproductservice.tx.PollingRawTransactionManager;
import at.schunker.mt.ethereumproductservice.util.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.tx.ChainIdLong;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:application-test.properties")
public class BaseContractTest {

    Logger logger = LogManager.getLogger(BaseContractTest.class);

    protected Web3j web3j = null;
    //protected String url = "http://localhost:7545";
    //protected String web3ClientVersionDescription = "EthereumJS TestRPC/v2.13.1/ethereum-js";

    //protected String url = "http://10.0.0.15:8545";
    //protected String web3ClientVersionDescription = "Geth/v1.10.3-stable-991384a7/windows-amd64/go1.16.3";

    // Infura
    @Value("${url}")
    protected String url;
    protected String web3ClientVersionDescription = "Geth/v1.10.6-omnibus-f00fbaa2/linux-amd64/go1.16.6";

    protected Map<String, Credentials> credentialMap = Map.of();
    protected int logBalanceCallCount = 0;

    @BeforeEach
    public void logBalance() {
        String methodName = ReflectionHelper.getMethodName(BaseContractTest.class);
        logger.info("==== {} ====", methodName);
        //logger.info("logBalance");

        if (this.credentialMap.isEmpty()) {
            logger.info("credential map is empty");
        }

        for (Map.Entry<String, Credentials> credentialsEntry : this.credentialMap.entrySet()) {
            String name = credentialsEntry.getKey();
            Credentials credentials = credentialsEntry.getValue();
            Request<?, EthGetBalance> getBalanceRequest = this.web3j.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST);
            EthGetBalance ethGetBalance;
            try {
                ethGetBalance = getBalanceRequest.send();
            } catch (Exception ex) {
                logger.error(ex.getMessage());
                continue;
            }

            BigDecimal etherBalance = Convert.fromWei(ethGetBalance.getBalance().toString(), Convert.Unit.ETHER);
            logger.info("{} {} balance: {}", name, credentials.getAddress(), etherBalance);
        }

        this.logBalanceCallCount++;
    }

    protected BigInteger getBalance(String address, DefaultBlockParameterName defaultBlockParameterName) {
        String methodName = ReflectionHelper.getMethodName(BaseContractTest.class);
        logger.info("==== {} ====", methodName);

        Request<?, EthGetBalance> ethGetBalanceContractRequest = this.web3j.ethGetBalance(address, defaultBlockParameterName);
        EthGetBalance ethGetBalance = null;
        try {
            ethGetBalance = ethGetBalanceContractRequest.send();
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.info(ex.getMessage());
            return null;
        }

        BigInteger balance = ethGetBalance.getBalance();
        return balance;
    }

    protected BigInteger getTransactionCount(String address, DefaultBlockParameterName defaultBlockParameterName) {
        String methodName = ReflectionHelper.getMethodName(BaseContractTest.class);
        logger.info("==== {} ====", methodName);

        Request<?, EthGetTransactionCount> getTransactionCountRequest = this.web3j.ethGetTransactionCount(address, defaultBlockParameterName);
        EthGetTransactionCount ethGetTransactionCount = null;
        try {
            ethGetTransactionCount = getTransactionCountRequest.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
        }

        BigInteger transactionCount = ethGetTransactionCount.getTransactionCount();
        return transactionCount;
    }

    protected org.web3j.protocol.core.methods.response.Transaction getTransactionForReceipt(TransactionReceipt transactionReceipt) {
        String methodName = ReflectionHelper.getMethodName(BaseContractTest.class);
        logger.info("==== {} ====", methodName);

        String transactionHash = transactionReceipt.getTransactionHash();
        Request<?, EthTransaction> request = this.web3j.ethGetTransactionByHash(transactionHash);
        EthTransaction ethTransaction = null;
        try {
            ethTransaction = request.send();
        } catch (Exception ex) {
            logger.error("exception: {}", ex.getMessage());
        }

        assertNotNull(ethTransaction);
        if (ethTransaction == null) {
            return null;
        }

        Optional<Transaction> optionalTransaction = ethTransaction.getTransaction();
        assertTrue(optionalTransaction.isPresent());
        if (!optionalTransaction.isPresent()) {
            return null;
        }

        org.web3j.protocol.core.methods.response.Transaction transaction = optionalTransaction.get();
        return transaction;
    }

    public Transaction getTransactionByHash(String transactionHash) {
        String methodName = ReflectionHelper.getMethodName(BaseContractTest.class);
        logger.info("==== {} ====", methodName);

        Request<?, EthTransaction> getTransactionByHashRequest = this.web3j.ethGetTransactionByHash(transactionHash);
        EthTransaction ethTransaction = null;
        try {
            ethTransaction = getTransactionByHashRequest.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{}", ex.getMessage());
        }

        Optional<Transaction> optionalTransaction = ethTransaction.getTransaction();
        if (!optionalTransaction.isPresent()) {
            return null;
        }

        return optionalTransaction.get();
    }

    public EthBlock.Block getBlockByNumber(DefaultBlockParameter defaultBlockParameter) {
        String methodName = ReflectionHelper.getMethodName(BaseContractTest.class);
        logger.info("==== {} ====", methodName);

        Request<?, EthBlock> getBlockRequest = this.web3j.ethGetBlockByNumber(defaultBlockParameter, false);
        EthBlock ethBlock = null;
        try {
            ethBlock = getBlockRequest.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            return null;
        }

        return ethBlock.getBlock();
    }

    /**
     * Cancels a pending transaction by replacing the pending transaction
     * by sending a new transaction with the same from address and nonce, a value of 0 Eth
     * and a higher gas fee to the from address itself.
     * @param credentials
     * @param nonce
     * @param gasLimit
     * @return transaction hash
     */
    protected String cancelTransaction(Credentials credentials, BigInteger nonce, BigInteger gasLimit) {
        String to = credentials.getAddress();
        BigInteger value = BigInteger.ZERO;

        Request<?, EthGasPrice> ethGasPriceRequest = this.web3j.ethGasPrice();
        EthGasPrice ethGasPrice = null;
        try {
            ethGasPrice = ethGasPriceRequest.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{}", ex.getMessage());
            return null;
        }

        BigInteger gasPrice = ethGasPrice.getGasPrice();
        double dGasPrice = gasPrice.doubleValue();
        dGasPrice = dGasPrice * 0.75;
        Double doubleGasPrice = dGasPrice;
        long longGasPrice = doubleGasPrice.longValue();
        gasPrice = BigInteger.valueOf(longGasPrice);

        //org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createEtherTransaction(from, nonce, gasPrice, gasLimit, to, value);
        //Request<?, EthSendTransaction> cancelTransactionRequest = this.web3j.ethSendTransaction(transaction);

        RawTransaction transaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, value);
        byte[] signedTransaction = TransactionEncoder.signMessage(transaction, ChainIdLong.MAINNET, credentials);
        String signedTransactionString = Numeric.toHexString(signedTransaction);

        Request<?, EthSendTransaction> cancelTransactionRequest = this.web3j.ethSendRawTransaction(signedTransactionString);
        EthSendTransaction sendTransaction = null;

        try {
            sendTransaction = cancelTransactionRequest.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{}", ex.getMessage());
            return null;
        }

        if (sendTransaction.hasError()) {
            Response.Error responseError = sendTransaction.getError();
            logger.error("{}", responseError.getMessage());
            return null;
        }

        return sendTransaction.getTransactionHash();
    }

    public String deployWithCustomNonce(String data, BigInteger gasLimit, BigInteger nonce, Credentials credentials) {
        String methodName = ReflectionHelper.getMethodName(BaseContractTest.class);
        logger.info("==== {} ====", methodName);

        BigInteger value = BigInteger.ZERO;
        //String data = ProductSale.BINARY; //+ encoded constructor
        /*
        Request<?, EthGasPrice> ethGasPriceRequest = this.web3j.ethGasPrice();
        EthGasPrice ethGasPrice = null;
        try {
            ethGasPrice = ethGasPriceRequest.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{}", ex.getMessage());
            return null;
        }

        BigInteger gasPrice = ethGasPrice.getGasPrice();
        logger.info("{} gas price: {}", methodName, gasPrice.toString());

        double doubleGasPrice = gasPrice.doubleValue();
        doubleGasPrice = doubleGasPrice * 0.75;
        Double dGasPrice = doubleGasPrice;
        long longGasPrice = dGasPrice.longValue();
        gasPrice = BigInteger.valueOf(longGasPrice);
        */

        BigInteger balance = this.getBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST);
        BigInteger gasPrice = balance.divide(gasLimit);

        //BigInteger gasPrice = BigInteger.valueOf(21000000000L); // 21 GWei

        logger.info("{} updated gas price: {}", methodName, gasPrice.toString());

        RawTransaction contractCreationTransaction = RawTransaction.createContractTransaction(nonce, gasPrice, gasLimit, value, data);
        byte[] signedTransaction = TransactionEncoder.signMessage(contractCreationTransaction, ChainIdLong.MAINNET, credentials);
        String signedTransactionString = Numeric.toHexString(signedTransaction);

        //this.url = "https://mainnet.infura.io/v3/b30d8f06ddb6423eb4d8bfaf5cf78e32";
        //this.setupTests();

        Request<?, EthSendTransaction> contractCreationTransactionRequest = this.web3j.ethSendRawTransaction(signedTransactionString);
        EthSendTransaction sendTransaction = null;

        try {
            sendTransaction = contractCreationTransactionRequest.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{}", ex.getMessage());
            return null;
        }

        if (sendTransaction.hasError()) {
            Response.Error responseError = sendTransaction.getError();
            logger.error("{}", responseError.getMessage());
            return null;
        }

        return sendTransaction.getTransactionHash();
    }

    public String deployWithPolling(String data, BigInteger gasLimit, Credentials credentials) {
        String methodName = ReflectionHelper.getMethodName(BaseContractTest.class);
        logger.info("==== {} ====", methodName);

        String to = null;
        BigInteger value = BigInteger.ZERO;
        //String data = ProductSale.BINARY; //+ encoded constructor
        BigInteger maxPriorityFeePerGas = BigInteger.valueOf(11000000000L); // 11 GWei
        BigInteger maxFeePerGas = BigInteger.ZERO;

        //to = credentials.getAddress();
        //data = null;

        PollingRawTransactionManager transactionManager = new PollingRawTransactionManager(this.web3j, credentials, ChainIdLong.MAINNET);
        //transactionManager.setNonce(nonce);
        //transactionManager.setGasPriceMultiplier(0.85);
        transactionManager.setGasPriceMultiplier(1);

        //EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor)
        EthSendTransaction sendTransaction = null;
        try {
            //sendTransaction = transactionManager.sendTransaction(BigInteger.ZERO, gasLimit, to, data, value);
            sendTransaction = transactionManager.sendEIP1559Transaction(0L, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, data, value);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            return null;
        }

        return sendTransaction.getTransactionHash();
    }
}
