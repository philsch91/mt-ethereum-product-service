package at.schunker.mt.ethereumproductservice.tx;

import at.schunker.mt.ethereumproductservice.util.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.exceptions.TxHashMismatchException;
import org.web3j.utils.Numeric;
import org.web3j.utils.TxHashVerifier;

import java.io.IOException;
import java.math.BigInteger;

public class PollingRawTransactionManager extends TransactionManager {

    Logger logger = LogManager.getLogger(PollingRawTransactionManager.class);

    protected Web3j web3j;
    protected Credentials credentials;
    protected long chainId;
    protected TxHashVerifier txHashVerifier;
    public boolean useTransactionReceiptProcessor = false;
    private BigInteger nonce;
    private double gasPriceMultiplier = 0.75;
    private BigInteger maxTransactionFee;
    private double maxPriorityFeePerGasMultiplier = 0.1;

    public BigInteger getNonce() {
        return this.nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public double getGasPriceMultiplier() {
        return this.gasPriceMultiplier;
    }

    public void setGasPriceMultiplier(double gasPriceMultiplier) {
        if (gasPriceMultiplier > 1) {
            throw new IllegalArgumentException();
        }
        this.gasPriceMultiplier = gasPriceMultiplier;
    }

    public BigInteger getMaxTransactionFee() {
        return this.maxTransactionFee;
    }

    public void setMaxTransactionFee(BigInteger maxTransactionFee) {
        this.maxTransactionFee = maxTransactionFee;
    }

    public double getMaxPriorityFeePerGasMultiplier() {
        return this.maxPriorityFeePerGasMultiplier;
    }

    public void setMaxPriorityFeePerGasMultiplier(double maxPriorityFeePerGasMultiplier) {
        if (maxPriorityFeePerGasMultiplier > 1) {
            throw new IllegalArgumentException();
        }
        this.maxPriorityFeePerGasMultiplier = maxPriorityFeePerGasMultiplier;
    }

    public PollingRawTransactionManager(Web3j web3j, Credentials credentials) {
        this(web3j, credentials, -1L);
    }

    public PollingRawTransactionManager(Web3j web3j, Credentials credentials, long chainId) {
        super(web3j, credentials.getAddress());
        this.web3j = web3j;
        this.credentials = credentials;
        this.chainId = chainId;
        this.txHashVerifier = new TxHashVerifier();
    }

    /**
     * Get hardcoded nonce or nonce via eth_getTransactionCount.
     * @return nonce
     */
    protected BigInteger getNonceIfNeeded() throws IOException {
        if (this.nonce != null) {
            return this.nonce;
        }

        // nonce via eth_getTransactionCount and DefaultBlockParameterName.PENDING
        Request<?, EthGetTransactionCount> getTransactionCountRequest = this.web3j.ethGetTransactionCount(this.credentials.getAddress(), DefaultBlockParameterName.PENDING);
        EthGetTransactionCount ethGetTransactionCount = getTransactionCountRequest.send();
        BigInteger transactionCount = ethGetTransactionCount.getTransactionCount();
        return transactionCount;
    }

    protected BigInteger getMaxTransactionFeeIfNeeded() throws IOException {
        if (this.maxTransactionFee != null) {
            return this.maxTransactionFee;
        }

        Request<?, EthGetBalance> ethGetBalanceContractRequest = this.web3j.ethGetBalance(this.credentials.getAddress(), DefaultBlockParameterName.LATEST);
        EthGetBalance ethGetBalance = ethGetBalanceContractRequest.send();
        BigInteger balance = ethGetBalance.getBalance();
        return balance;
    }

    protected BigInteger getBlockBaseFeePerGas(DefaultBlockParameter defaultBlockParameter) throws IOException {
        Request<?, EthBlock> getBlockRequest = this.web3j.ethGetBlockByNumber(defaultBlockParameter, false);
        EthBlock ethBlock = getBlockRequest.send();
        EthBlock.Block block = ethBlock.getBlock();
        //logger.info("{} latest block logs bloom: {}", methodName, block.getLogsBloom());
        /*
        String baseFee = block.getBaseFeePerGas();
        if (baseFee.startsWith("0x")) {
            baseFee = baseFee.substring(2);
        }
        BigInteger baseFeePerGas = new BigInteger(baseFee, 16);
        */
        BigInteger baseFeePerGas = block.getBaseFeePerGas();

        return baseFeePerGas;
    }

    protected BigInteger getMaxPriorityFeePerGasIfNeeded(BigInteger maxFeePerGas) {
        double dMaxPriorityFeePerGas = maxFeePerGas.doubleValue();
        dMaxPriorityFeePerGas = dMaxPriorityFeePerGas * this.maxPriorityFeePerGasMultiplier;
        Double doubleMaxPriorityFeePerGas = dMaxPriorityFeePerGas;
        long longMaxPriorityFeePerGas = doubleMaxPriorityFeePerGas.longValue();
        BigInteger maxPriorityFeePerGas = BigInteger.valueOf(longMaxPriorityFeePerGas);
        return maxPriorityFeePerGas;
    }

    protected RawTransaction createRawTransaction(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, BigInteger value, String data) {
        if (to == null || to.equals("")) {
            RawTransaction transaction = RawTransaction.createContractTransaction(nonce, gasPrice, gasLimit, value, data);
            return transaction;
        }

        if (data == null || data.equals("")) {
            RawTransaction transaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, value);
            return transaction;
        }

        RawTransaction transaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data);
        return transaction;
    }

    public String signRawTransaction(RawTransaction rawTransaction) {
        byte[] signedTransaction;
        if (this.chainId > -1L) {
            signedTransaction = TransactionEncoder.signMessage(rawTransaction, this.chainId, this.credentials);
        } else {
            signedTransaction = TransactionEncoder.signMessage(rawTransaction, this.credentials);
        }

        String signedTransactionString = Numeric.toHexString(signedTransaction);
        return signedTransactionString;
    }

    public EthSendTransaction sendSignedTransaction(String signedTransaction) throws IOException {
        Request<?, EthSendTransaction> transactionRequest = this.web3j.ethSendRawTransaction(signedTransaction);
        EthSendTransaction ethSendTransaction = transactionRequest.send();
        //EthSendTransaction ethSendTransaction = new EthSendTransaction();
        //sendTransaction.setResult("");

        if (ethSendTransaction == null) {
            return null;
        }

        if (ethSendTransaction.hasError()) {
            Response.Error responseError = ethSendTransaction.getError();
            logger.error("{}", responseError.getMessage());
            throw new RuntimeException("Error processing transaction request: " + responseError.getMessage());
        }

        String txHashLocal = Hash.sha3(signedTransaction);
        String txHashRemote = ethSendTransaction.getTransactionHash();
        if (!this.txHashVerifier.verify(txHashLocal, txHashRemote)) {
            throw new TxHashMismatchException(txHashLocal, txHashRemote);
        }

        return ethSendTransaction;
    }

    protected TransactionReceipt executeTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException, TransactionException {
        return this.executeTransaction(gasPrice, gasLimit, to, data, value, false);
    }

    protected TransactionReceipt executeTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException, TransactionException {
        if (this.useTransactionReceiptProcessor) {
            return super.executeTransaction(gasPrice, gasLimit, to, data, value, constructor);
        }

        EthSendTransaction ethSendTransaction = this.sendTransaction(gasPrice, gasLimit, to, data, value, constructor);
        TransactionReceipt transactionReceipt = new TransactionReceipt();
        transactionReceipt.setTransactionHash(ethSendTransaction.getTransactionHash());
        return transactionReceipt;
    }

    protected TransactionReceipt executeTransactionEIP1559(long chainId, BigInteger maxPriorityFeePerGas, BigInteger maxFeePerGas, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException, TransactionException {
        return this.executeTransactionEIP1559(chainId, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, data, value, false);
    }

    protected TransactionReceipt executeTransactionEIP1559(long chainId, BigInteger maxPriorityFeePerGas, BigInteger maxFeePerGas, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException, TransactionException {
        if (this.useTransactionReceiptProcessor) {
            return super.executeTransactionEIP1559(chainId, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, data, value, constructor);
        }

        EthSendTransaction ethSendTransaction = this.sendEIP1559Transaction(chainId, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, data, value, constructor);
        TransactionReceipt transactionReceipt = new TransactionReceipt();
        transactionReceipt.setTransactionHash(ethSendTransaction.getTransactionHash());
        return transactionReceipt;
    }

    /*
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException {
        return this.sendTransaction(gasPrice, gasLimit, to, data, value, false);
    } */

    /**
     * Overrides abstract sendTransaction method in TransactionManager class.
     * @param gasPrice
     * @param gasLimit
     * @param to
     * @param data
     * @param value
     * @param constructor
     * @return
     * @throws IOException
     */
    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        String methodName = ReflectionHelper.getMethodName(PollingRawTransactionManager.class);
        logger.info("==== {} ====", methodName);

        BigInteger nonce = this.getNonceIfNeeded();

        BigInteger maxTransactionFee = BigInteger.ZERO;
        BigInteger transactionFee = BigInteger.ONE;

        while (maxTransactionFee.compareTo(transactionFee) < 0) {
            maxTransactionFee = this.getMaxTransactionFeeIfNeeded();
            logger.info("{} available balance (max theoretical transaction fee): {} (Wei)", methodName, maxTransactionFee.toString());

            Request<?, EthGasPrice> ethGasPriceRequest = this.web3j.ethGasPrice();
            EthGasPrice ethGasPrice = ethGasPriceRequest.send();
            gasPrice = ethGasPrice.getGasPrice();

            double dGasPrice = gasPrice.doubleValue();
            dGasPrice = dGasPrice * this.gasPriceMultiplier;
            Double doubleGasPrice = dGasPrice;
            long longGasPrice = doubleGasPrice.longValue();
            gasPrice = BigInteger.valueOf(longGasPrice);
            logger.info("{} gas price: {}", methodName, gasPrice.toString());

            transactionFee = gasLimit.multiply(gasPrice);
            logger.info("{} transaction fee: {}", methodName, transactionFee.toString());

            BigInteger costDifference = transactionFee.subtract(maxTransactionFee);
            logger.info("{} transaction fee - max transaction fee: {}", methodName, costDifference);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException();
            }
        }

        RawTransaction transaction = this.createRawTransaction(nonce, gasPrice, gasLimit, to, value, data);
        String signedTransaction = this.signRawTransaction(transaction);
        EthSendTransaction ethSendTransaction = this.sendSignedTransaction(signedTransaction);

        return ethSendTransaction;
    }

    public EthSendTransaction sendEIP1559Transaction(long chainId, BigInteger maxPriorityFeePerGas, BigInteger maxFeePerGas, BigInteger gasLimit, String to, String data, BigInteger value) throws IOException {
        return this.sendEIP1559Transaction(chainId, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, data, value, false);
    }

    /**
     * Override abstract sendEIP1559Transaction method in TransactionManager class.
     * The argument maxFeePerGas is not used.
     * @param chainId
     * @param maxPriorityFeePerGas
     * @param maxFeePerGas
     * @param gasLimit
     * @param to
     * @param data
     * @param value
     * @param constructor
     * @return
     * @throws IOException
     */
    @Override
    public EthSendTransaction sendEIP1559Transaction(long chainId, BigInteger maxPriorityFeePerGas, BigInteger maxFeePerGas, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        String methodName = ReflectionHelper.getMethodName(PollingRawTransactionManager.class);
        logger.info("==== {} ====", methodName);

        if (chainId == 0L) {
            chainId = this.chainId;
        }

        BigInteger nonce = this.getNonceIfNeeded();

        BigInteger maxTransactionFee = BigInteger.ZERO;
        BigInteger transactionFee = BigInteger.ONE;

        while (maxTransactionFee.compareTo(transactionFee) < 0) {
            maxTransactionFee = this.getMaxTransactionFeeIfNeeded();
            logger.info("{} available balance (max theoretical transaction fee): {} (Wei)", methodName, maxTransactionFee.toString());

            BigInteger baseFeePerGas = this.getBlockBaseFeePerGas(DefaultBlockParameterName.LATEST);
            logger.info("{} latest block base fee per gas: {}", methodName, baseFeePerGas.toString());

            double dBaseFeePerGas = baseFeePerGas.doubleValue();
            dBaseFeePerGas = dBaseFeePerGas * this.gasPriceMultiplier;
            Double doubleBaseFeePerGas = dBaseFeePerGas;
            long longBaseFeePerGas = doubleBaseFeePerGas.longValue();
            baseFeePerGas = BigInteger.valueOf(longBaseFeePerGas);
            logger.info("{} base fee per gas: {}", methodName, baseFeePerGas.toString());

            maxFeePerGas = maxTransactionFee.divide(gasLimit);

            // optional check of maxFeePerGas
            if (maxFeePerGas.compareTo(baseFeePerGas) < 0) {
                logger.error("{} max fee per gas ({}) is lower than base fee per gas ({}) - invalid transaction", methodName, maxFeePerGas.toString(), baseFeePerGas.toString());
                maxTransactionFee = BigInteger.ZERO;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException();
                }
                continue;
            }
            // optional check of maxFeePerGas end

            if (maxPriorityFeePerGas == null) {
                maxPriorityFeePerGas = this.getMaxPriorityFeePerGasIfNeeded(maxFeePerGas);
                logger.info("{} max priority fee per gas : {}", methodName, maxPriorityFeePerGas.toString());
            }

            // optional check of maxPriorityFee
            BigInteger offeredBaseFeePerGas = maxFeePerGas.subtract(maxPriorityFeePerGas);
            BigInteger reducedMaxPriorityFeePerGas = BigInteger.valueOf(maxPriorityFeePerGas.longValue());
            // maxPriorityFeePerGas is too high
            // maxPriorityFeePerGas will be automatically reduced for transaction
            // to remain valid by the protocol
            if (offeredBaseFeePerGas.compareTo(baseFeePerGas) < 0) {
                reducedMaxPriorityFeePerGas = maxFeePerGas.subtract(baseFeePerGas);
                logger.info("{} reduced max priority fee per gas (protocol): {}", methodName, reducedMaxPriorityFeePerGas.toString());
            }
            // optional check of maxPriorityFee end

            transactionFee = gasLimit.multiply(baseFeePerGas);
            logger.info("{} minimum transaction fee: {}", methodName, transactionFee.toString());

            BigInteger maxPriorityFee = gasLimit.multiply(reducedMaxPriorityFeePerGas);
            logger.info("{} maximum priority fee: {}", methodName, maxPriorityFee.toString());

            // maxTransactionFee - (baseFee + maxPriorityFee) = returned to user
            transactionFee = transactionFee.add(maxPriorityFee);
            logger.info("{} transaction fee including max priority fee: {}", methodName, transactionFee.toString());

            BigInteger costDifference = transactionFee.subtract(maxTransactionFee);
            logger.info("{} transaction fee - max transaction fee: {}", methodName, costDifference);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException();
            }
        }

        RawTransaction transaction = RawTransaction.createTransaction(chainId, nonce, gasLimit, to, value, data, maxPriorityFeePerGas, maxFeePerGas);
        String signedTransaction = this.signRawTransaction(transaction);
        EthSendTransaction ethSendTransaction = this.sendSignedTransaction(signedTransaction);

        return ethSendTransaction;
    }

    /**
     * The parameter gasPremium is used for maxPriorityFeePerGas.
     * The parameter feeCap is used for maxFeePerGas.
     * @param gasPremium
     * @param feeCap
     * @param gasLimit
     * @param to
     * @param data
     * @param value
     * @param constructor
     * @return
     * @throws IOException
     */
    public EthSendTransaction sendTransactionEIP1559(BigInteger gasPremium, BigInteger feeCap, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        String methodName = ReflectionHelper.getMethodName(PollingRawTransactionManager.class);
        logger.info("==== {} ====", methodName);

        BigInteger nonce = this.getNonceIfNeeded();

        // org.web3j.core 4.8.2
        //RawTransaction transaction = RawTransaction.createTransaction(nonce, (BigInteger)null, gasLimit, to, value, data, gasPremium, feeCap);
        RawTransaction transaction = RawTransaction.createTransaction(this.chainId, nonce, gasLimit, to, value, data, gasPremium, feeCap);

        String signedTransaction = this.signRawTransaction(transaction);
        EthSendTransaction ethSendTransaction = this.sendSignedTransaction(signedTransaction);

        return ethSendTransaction;
    }

    @Override
    public String sendCall(String s, String s1, DefaultBlockParameter defaultBlockParameter) throws IOException {
        return null;
    }

    @Override
    public EthGetCode getCode(String s, DefaultBlockParameter defaultBlockParameter) throws IOException {
        return null;
    }
}
