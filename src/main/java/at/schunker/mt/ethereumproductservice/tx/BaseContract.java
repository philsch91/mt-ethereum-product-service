package at.schunker.mt.ethereumproductservice.tx;

import at.schunker.mt.ethereumproductservice.contract.ProductSale;
import at.schunker.mt.ethereumproductservice.tx.EstimatedContractGasProvider;
import at.schunker.mt.ethereumproductservice.tx.EstimatedGasProvider;
import org.web3j.abi.FunctionEncoder;
import org.web3j.crypto.Credentials;
import org.web3j.ens.EnsResolver;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.RevertReasonExtractor;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigInteger;

public class BaseContract extends Contract {

    private boolean testMode = false;

    protected BaseContract(String contractBinary, String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider gasProvider) {
        super(contractBinary, contractAddress, web3j, transactionManager, gasProvider);
    }

    protected BaseContract(EnsResolver ensResolver, String contractBinary, String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider gasProvider) {
        super(ensResolver, contractBinary, contractAddress, web3j, transactionManager, gasProvider);
    }

    protected BaseContract(String contractBinary, String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        super(contractBinary, contractAddress, web3j, credentials, gasProvider);
    }

    @Deprecated
    protected BaseContract(String contractBinary, String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(contractBinary, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    @Deprecated
    protected BaseContract(String contractBinary, String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(contractBinary, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    protected BaseContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    @Deprecated
    protected BaseContract(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public boolean isTestMode() {
        return this.testMode;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /**
     * Overrides protected Contract.executeTransaction(Function function)
     * @param function
     * @return
     * @throws IOException
     * @throws TransactionException
     */
    protected TransactionReceipt executeTransaction(org.web3j.abi.datatypes.Function function) throws IOException, TransactionException {
        //return this.executeTransaction(function, BigInteger.ZERO);
        //System.out.println(this.getClass().getSimpleName() + ".executeTransaction(Function function)");
        System.out.println("BaseContract" + ".executeTransaction(Function function)");

        String data = FunctionEncoder.encode(function);

        ContractGasProvider contractGasProvider = this.gasProvider;
        BigInteger gasPrice = contractGasProvider.getGasPrice(function.getName());
        //BigInteger gasLimit = contractGasProvider.getGasLimit(funcName);
        BigInteger gasLimit = BigInteger.ZERO;

        System.out.println(contractGasProvider.getClass().toString());
        //if (EstimatedGasProvider.class.isInstance(contractGasProvider))) {
        if (contractGasProvider.getClass().equals(EstimatedGasProvider.class)) {
            System.out.println("detected EstimatedContractGasProvider");
            String fromAddress = this.transactionManager.getFromAddress();
            EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)this.gasProvider;
            gasLimit = estimatedContractGasProvider.getGasLimit(fromAddress, gasPrice, this.contractAddress, BigInteger.ZERO, data);
        } else {
            gasLimit = contractGasProvider.getGasLimit(function.getName());
        }

        if (this.testMode) {
            System.out.println("BaseContract.testMode = true");
            TransactionReceipt transactionReceipt = new TransactionReceipt();
            return transactionReceipt;
        }

        EthSendTransaction transactionResponse = this.transactionManager.sendTransaction(gasPrice, gasLimit, this.contractAddress, data, BigInteger.ZERO);
        if (transactionResponse.hasError()) {
            throw new RuntimeException(transactionResponse.getError().getMessage());
        }

        String transactionHash = transactionResponse.getTransactionHash();
        TransactionReceiptProcessor transactionReceiptProcessor = new PollingTransactionReceiptProcessor(this.web3j, 10000, 10);
        return transactionReceiptProcessor.waitForTransactionReceipt(transactionHash);
    }

    /**
     * Overrides private Contract.executeTransaction(Function function, BigInteger weiValue)
     * Not called because private methods cannot be overriden.
     * @param function
     * @param weiValue
     * @return the transaction receipt for the transaction
     */
    protected TransactionReceipt executeTransaction(org.web3j.abi.datatypes.Function function, BigInteger weiValue) throws IOException, TransactionException {
        //System.out.println(this.getClass().getSimpleName() + ".executeTransaction(Function function, BigInteger weiValue)");
        System.out.println("BaseContract" + ".executeTransaction(Function function, BigInteger weiValue)");
        return this.executeTransaction(FunctionEncoder.encode(function), weiValue, function.getName(), false);
    }

    /**
     * Overrides package-private Contract.executeTransaction
     * The lack of an access modifier for Contract.executeTransaction
     * results in that the method can only be called within the class instance
     * or the package.
     * Called by Contract.deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider).
     * Not called because private methods cannot be overriden.
     * @param data
     * @param weiValue
     * @param funcName
     * @param constructor
     * @return the transaction receipt for the transaction
     * @throws TransactionException
     * @throws IOException
     */
    public TransactionReceipt executeTransaction(String data, BigInteger weiValue, String funcName, boolean constructor) throws TransactionException, IOException {
        //System.out.println(this.getClass().getSimpleName() + ".executeTransaction(String data, BigInteger weiValue, String funcName, boolean constructor)");
        System.out.println("BaseContract" + ".executeTransaction(String data, BigInteger weiValue, String funcName, boolean constructor)");

        ContractGasProvider contractGasProvider = this.gasProvider;
        BigInteger gasPrice = contractGasProvider.getGasPrice(funcName);
        //BigInteger gasLimit = contractGasProvider.getGasLimit(funcName);
        BigInteger gasLimit = BigInteger.ZERO;

        System.out.println(contractGasProvider.getClass().toString());
        //if (EstimatedGasProvider.class.isInstance(contractGasProvider))) {
        if (contractGasProvider.getClass().equals(EstimatedGasProvider.class)) {
            System.out.println("detected EstimatedContractGasProvider");
            String fromAddress = this.transactionManager.getFromAddress();
            EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)this.gasProvider;
            gasLimit = estimatedContractGasProvider.getGasLimit(fromAddress, gasPrice, this.contractAddress, weiValue, data);
        } else {
            gasLimit = contractGasProvider.getGasLimit(funcName);
        }

        TransactionReceipt receipt = this.send(this.contractAddress, data, weiValue, gasPrice, gasLimit, constructor);
        if (!receipt.isStatusOK()) {
            throw new TransactionException(String.format("Transaction %s has failed with status: %s. Gas used: %s. Revert reason: '%s'.", receipt.getTransactionHash(), receipt.getStatus(), receipt.getGasUsedRaw() != null ? receipt.getGasUsed().toString() : "unknown", RevertReasonExtractor.extractRevertReason(receipt, data, this.web3j, true)), receipt);
        } else {
            return receipt;
        }
    }

    public static <T extends Contract> Class<T> convert(Class clazz) {
        return clazz;
    }
    /*
    public static <T extends Contract> TransactionReceipt deployContract(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) throws RuntimeException, IOException, TransactionException {
        //Class<T> type = ProductSale.convert(ProductSale.class);
        Class<T> type = (Class<T>)ProductSale.class;
        Constructor<T> constructor;
        T contract;
        try {
            ////constructor = type.getDeclaredConstructor(String.class, Web3j.class, Credentials.class, ContractGasProvider.class);
            //constructor = (Constructor<T>)T.class.getDeclaredConstructor(String.class, Web3j.class, Credentials.class, ContractGasProvider.class);
            constructor = (Constructor<T>)ProductSale.class.getDeclaredConstructor(String.class, Web3j.class, Credentials.class, ContractGasProvider.class);
            constructor.setAccessible(true);
            contract = (T) constructor.newInstance(null, web3j, credentials, contractGasProvider);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }

        //deploy(type, this.web3j, credentials, contractGasProvider, BINARY, "", BigInteger.ZERO);
        //create(contract, binary, encodedConstructor, value);
        ProductSale productSale = (ProductSale)contract;
        ////((ProductSale) contract).BINARY
        TransactionReceipt transactionReceipt = productSale.executeTransaction(BINARY + "", BigInteger.ZERO, "deploy", true);
        return transactionReceipt;
    }
    */

    /**
     *
     * @return protected field Contract.gasProvider
     */
    public ContractGasProvider getGasProvider() {
        return this.gasProvider;
    }
}
