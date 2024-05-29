package at.schunker.mt.ethereumproductservice.tx;

import at.schunker.mt.ethereumproductservice.util.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.transaction.type.Transaction1559;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.tx.ChainIdLong;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

public class EstimatedGasProvider implements EstimatedContractGasProvider {

    Logger logger = LogManager.getLogger(EstimatedGasProvider.class);

    private final static BigInteger MINIMUM_GASLIMIT = BigInteger.valueOf(21000L);
    private final Web3j web3j;
    private Transaction transaction = null;
    private BigInteger defaultGasPrice = BigInteger.valueOf(20000000000L);
    private BigInteger defaultGasLimit = BigInteger.valueOf(6721975L);
    private BigInteger lastReturnedGasPrice = null;
    private BigInteger lastReturnedGasLimit = null;
    private double gasPriceMultiplier = 1;
    private boolean useBaseFee = false;

    public EstimatedGasProvider(Web3j web3j) {
        this.web3j = web3j;
    }

    /** EstimatedContractGasProvider Accessors */

    public BigInteger getDefaultGasPrice() {
        return this.defaultGasPrice;
    }

    public void setDefaultGasPrice(BigInteger defaultGasPrice) {
        this.defaultGasPrice = defaultGasPrice;
    }

    public BigInteger getDefaultGasLimit() {
        return this.defaultGasLimit;
    }

    public void setDefaultGasLimit(BigInteger defaultGasLimit) {
        this.defaultGasLimit = defaultGasLimit;
    }

    public boolean useBaseFee() {
        return this.useBaseFee;
    }

    public void setUseBaseFee(boolean useBaseFee) {
        this.useBaseFee = useBaseFee;
    }

    /** EstimatedGasProvider Accessors */

    public Transaction getTransaction() {
        return this.transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public BigInteger getLastReturnedGasPrice() {
        return this.lastReturnedGasPrice;
    }

    public BigInteger getLastReturnedGasLimit() {
        return this.lastReturnedGasLimit;
    }

    public void setGasPriceMultiplier(double multiplier) {
        this.gasPriceMultiplier = multiplier;
    }

    /** EstimatedGasProvider */

    private BigInteger addGasPriceMultiplier(BigInteger gasPrice) {
        String methodName = ReflectionHelper.getMethodName(this.getClass());
        //BigInteger additionalGasPrice = gasPrice.multiply(this.gasPriceMultiplier);
        double doubleGasPrice = gasPrice.doubleValue();
        //logger.info(String.format("%s doubleGasPrice %f", methodName, doubleGasPrice));
        doubleGasPrice = doubleGasPrice * this.gasPriceMultiplier;
        //logger.info(String.format("%s doubleGasPrice %f", methodName, doubleGasPrice));
        // double to long via explicit cast
        //long longGasPrice = (long)doubleGasPrice;
        // double to long via boxing
        Double dGasPrice = doubleGasPrice;
        long longGasPrice = dGasPrice.longValue();
        //logger.info(String.format("%s longGasPrice %d", methodName, longGasPrice));
        // use if gasPriceMultiplier < 1
        //BigInteger additionalGasPrice = BigInteger.valueOf(longGasPrice);
        //gasPrice = gasPrice.add(additionalGasPrice);
        gasPrice = BigInteger.valueOf(longGasPrice);
        return gasPrice;
    }

    protected BigInteger getBlockBaseFeePerGas(DefaultBlockParameter defaultBlockParameter) throws IOException {
        String methodName = ReflectionHelper.getMethodName(this.getClass());
        methodName = "getBlockBaseFeePerGas";
        logger.info("{}", methodName);
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

        logger.info("{} base fee per gas: {}", methodName, baseFeePerGas.toString());
        return baseFeePerGas;
    }

    private BigInteger getLatestGasLimit() throws IOException {
        String methodName = ReflectionHelper.getMethodName(this.getClass());
        methodName = "getLatestGasLimit";
        logger.info("{}", methodName);
        BigInteger gasLimit = this.defaultGasLimit;
        if (gasLimit != null) {
            return gasLimit;
        }

        Request<?, EthBlock> ethGetBlockRequest = this.web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false);
        EthBlock ethBlock = ethGetBlockRequest.send();
        gasLimit = ethBlock.getBlock().getGasLimit();

        logger.info("{} gas limit: {}", methodName, gasLimit.toString());
        return gasLimit;
    }

    /** ContractGasProvider */

    /**
     * Get the {@code gas price} value for the transaction
     * The gas price value is independent of the transaction
     * Considered @deprecated in PR #973
     * @param contractFunctionName to adjust the gas price
     *                             for execution time handling
     * @return the gas price for the transaction
     */
    @Override
    public BigInteger getGasPrice(String contractFunctionName) {
        String methodName = ReflectionHelper.getMethodName(this.getClass());
        //System.out.println(this.getClass().getSimpleName() + " " + methodName);
        //System.out.println(this.getClass().getSimpleName() + ".getGasPrice(String contractFunctionName)");
        logger.info("getGasPrice(String contractFunctionName)");
        logger.info("contractFunctionName: " + contractFunctionName);
        BigInteger gasPrice = this.defaultGasPrice;

        //TODO: check if condition for transaction can be removed
        if (this.transaction != null) {
            gasPrice = this.getGasPrice();
        }

        gasPrice = this.addGasPriceMultiplier(gasPrice);
        logger.info("getGasPrice(String contractFunctionName) gas price: {}", gasPrice);
        this.lastReturnedGasPrice = gasPrice;
        return gasPrice;
    }

    /**
     * Get the {@code gas price} value for the transaction
     * The gas price value is independent of the transaction
     * currently not used by default
     * @return the gas price for the transaction
     */
    @Override
    public BigInteger getGasPrice() {
        String methodName = ReflectionHelper.getMethodName(this.getClass());
        //System.out.println(this.getClass().getSimpleName() + " " + methodName);
        //System.out.println(this.getClass().getSimpleName() + ".getGasPrice()");
        logger.info(methodName);
        EthGasPrice ethGasPrice = null;
        try {
            Request<?, EthGasPrice> request = this.web3j.ethGasPrice();
            ethGasPrice = request.send();
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            return BigInteger.ZERO;
        }

        BigInteger gasPrice = ethGasPrice.getGasPrice();
        logger.info("{} gas price: {}", methodName, gasPrice);

        return gasPrice;
    }

    /**
     * Get the {@code gas limit} value for the transaction
     * The gas limit value depends on the transaction
     * @param contractFunctionName to calculate the gas limit
     *                             that is needed for the transaction
     * @return the gas limit for the transaction
     */
    @Override
    public BigInteger getGasLimit(String contractFunctionName) {
        //String methodName = ReflectionHelper.getMethodName(this.getClass());
        //System.out.println(this.getClass().getSimpleName() + " " + methodName);
        //System.out.println(this.getClass().getSimpleName() + ".getGasLimit(String contractFunctionName)");
        logger.info("getGasLimit(String contractFunctionName)");
        logger.info("contractFunctionName: " + contractFunctionName);
        BigInteger gasLimit = this.defaultGasLimit;
        /*
        if (contractFunctionName == null || this.transaction != null) {
            return this.getGasLimit();
        } */

        if (this.transaction == null) {
            logger.info("getGasLimit(String contractFunctionName) gas limit: {}", gasLimit);
            this.lastReturnedGasLimit = gasLimit;
            return gasLimit;
        }

        gasLimit = this.getGasLimit();
        this.lastReturnedGasLimit = gasLimit;
        return gasLimit;
    }

    /**
     * Get the {@code gas limit} value for the transaction.
     * Called if this.transaction is not null.
     * The gas limit value depends on the transaction
     * currently not used by default
     * @return the gas limit for the transaction
     */
    @Override
    public BigInteger getGasLimit() {
        String methodName = ReflectionHelper.getMethodName(this.getClass());
        //System.out.println(this.getClass().getSimpleName() + " " + methodName);
        //System.out.println(this.getClass().getSimpleName() + ".getGasLimit()");
        logger.info(methodName);
        if (this.transaction == null) {
            return this.defaultGasLimit;
        }
        // from
        String fromAddress = this.transaction.getFrom();
        // gasPrice
        //System.out.println(this.transaction.getGasPrice());
        logger.info("{} gas price: {}", methodName, this.transaction.getGasPrice());
        String strGasPrice = this.transaction.getGasPrice();
        if (strGasPrice.startsWith("0x")) {
            strGasPrice = strGasPrice.substring(2);
        }
        BigInteger gasPrice = new BigInteger(strGasPrice, 16);
        // to
        String toAddress = this.transaction.getTo();
        // value
        //System.out.println(this.transaction.getValue());
        logger.info("{} transaction value: {}", methodName, this.transaction.getValue());
        String strValue = this.transaction.getValue();
        if (strValue.startsWith("0x")) {
            strValue = strValue.substring(2);
        }
        BigInteger transactionValue = new BigInteger(strValue, 16);
        // data
        String data = this.transaction.getData();

        BigInteger gasLimit;
        try {
            gasLimit = this.getGasLimit(fromAddress, gasPrice, toAddress, transactionValue, data);
        } catch (IOException ioex) {
            ioex.printStackTrace();
            logger.error(ioex.getMessage());
            return null;
        }

        return gasLimit;
    }

    /** EstimatedContractGasProvider */

    /**
     * Get the {@code gas limit} value for the transaction
     * The gas limit value depends on the transaction
     * Directly called by BaseContract subclasses
     * @param fromAddress
     * @param gasPrice
     * @param contractAddress
     * @param weiValue
     * @param data
     * @return
     * @throws IOException
     */
    @Override
    public BigInteger getGasLimit(String fromAddress, BigInteger gasPrice, String contractAddress, BigInteger weiValue, String data) throws IOException {
        String methodName = ReflectionHelper.getMethodName(this.getClass());
        //System.out.println(this.getClass().getSimpleName() + " " + methodName);
        logger.info("getGasLimit(String fromAddress, BigInteger gasPrice, ...)");
        // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
        logger.info("from: {}, gasPrice: {}, contractAddress: {}, value: {}, data: {}", fromAddress, gasPrice, contractAddress, weiValue, data);
        // nonce
        BigInteger nonce = BigInteger.ZERO;
        // randomized nonce
        //int randomInt = new Random().nextInt() & Integer.MAX_VALUE;
        //logger.info("random nonce: {}", randomInt);
        //nonce = BigInteger.valueOf(randomInt);

        // nonce via eth_getTransactionCount and DefaultBlockParameterName.PENDING
        Request<?, EthGetTransactionCount> ethGetTransactionCountRequest = this.web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.PENDING);
        EthGetTransactionCount ethGetTransactionCount = ethGetTransactionCountRequest.send();
        BigInteger transactionCount = ethGetTransactionCount.getTransactionCount();
        //nonce = transactionCount.add(BigInteger.ONE);
        nonce = transactionCount;
        logger.info("getGasLimit(String fromAddress, BigInteger gasPrice, ...) nonce: {}", nonce.toString());
        // gas limit
        BigInteger gasLimit = this.getLatestGasLimit();

        if (this.useBaseFee()) {
            // max fee per gas less than block base fee
            // maxFeePerGas: 20000000000 baseFee: 23346538503 (supplied gas 377624)
            gasPrice = this.getBlockBaseFeePerGas(DefaultBlockParameterName.LATEST);
            this.setGasPriceMultiplier(1.1);
            gasPrice = this.addGasPriceMultiplier(gasPrice);
            this.setGasPriceMultiplier(1.0);
        }

        // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
        Transaction estimatedTransaction = new Transaction(
                fromAddress, nonce, gasPrice, gasLimit,
                contractAddress, weiValue, data);
        /*
        BigInteger blockBaseFeePerGas = this.getBlockBaseFeePerGas(DefaultBlockParameterName.LATEST);
        BigInteger maxPriorityFeePerGas = BigInteger.ZERO;
        estimatedTransaction = new Transaction(fromAddress, nonce, gasPrice, gasLimit, contractAddress, weiValue, data, ChainIdLong.MAINNET, maxPriorityFeePerGas, blockBaseFeePerGas);
        //Transaction1559.createTransaction(chainId, nonce, gasLimit, to, value, data, maxPriorityFeePerGas, maxFeePerGas);
        */

        Request<?, EthEstimateGas> request = this.web3j.ethEstimateGas(estimatedTransaction);
        EthEstimateGas estimateGas = request.send();
        //logger.info("estimate gas: {}", estimateGas.toString());
        BigInteger gasUsed = estimateGas.getAmountUsed();
        //System.out.println(this.getClass().getSimpleName() + " estimated gas limit: " + gasUsed);
        logger.info("getGasLimit(String fromAddress, BigInteger gasPrice, ...) estimated gas limit (amount used): {}", gasUsed);
        this.lastReturnedGasLimit = gasUsed;

        return gasUsed;
    }

    /** EstimatedGasProvider GasPriceMultiplier */

    public enum GasPriceMultiplier {
        ONE, TENPERCENT, TWENTYPERCENT, THIRTYPERCENT, FOURTYPERCENT;

        public static int getGasPriceMultiplierId(String name) {
            try {
                return GasPriceMultiplier.valueOf(name).ordinal();
            } catch (IllegalArgumentException ex) {
                return -1;
            }
        }

        public double getGasPriceMultiplier() {
            return this.ordinal() * 0.1 + 1;
        }
    }
}
