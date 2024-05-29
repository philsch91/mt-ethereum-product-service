package at.schunker.mt.ethereumproductservice.contract;

import at.schunker.mt.ethereumproductservice.tx.PollingRawTransactionManager;
import at.schunker.mt.ethereumproductservice.util.ReflectionHelper;
import at.schunker.mt.ethereumproductservice.logging.HttpLogger;
import at.schunker.mt.ethereumproductservice.tx.EstimatedGasProvider;
import io.reactivex.Flowable;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.*;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple7;
import org.web3j.tx.ChainIdLong;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
class ProductSaleTest {

    Logger logger = LogManager.getLogger(ProductSaleTest.class);

    private static final BigInteger ORDER_PRICE_TYPE = BigInteger.ONE;
    private static final BigInteger SHIPMENT_PRICE_TYPE = BigInteger.TWO;
    private static final BigInteger DELIVERY_DATE = BigInteger.valueOf(1631923200L);

    private Web3j web3j;

    @Value("${url}")
    protected String url;
    //private String url = "http://localhost:7545";
    //private String url = "http://10.0.0.15:8545";

    @Value("${webClientVersionDescription}")
    private String web3ClientVersionDescription;
    //private String web3ClientVersionDescription = "EthereumJS TestRPC/v2.13.1/ethereum-js";
    //private String web3ClientVersionDescription = "Geth/v1.10.6-omnibus-f00fbaa2/linux-amd64/go1.16.6";
    //private String web3ClientVersionDescription = "Geth/v1.10.6-stable-576681f2/windows-amd64/go1.16.4";

    private ProductSale productSale;
    @Value("${productsalecontract.address:notset}")
    private String contractAddress;

    @Value("${seller.privatekey}")
    private String sellerPrivateKey;
    @Value("${buyer.privatekey:default}")
    private String buyerPrivateKey;
    @Value("${courier.privatekey:default}")
    private String courierPrivateKey;

    // BIP-39 (+ BIP-32 & BIP-44)
    @Value("${seller.mnemonic:notset}")
    private String sellerMnemonic;
    @Value("${buyer.password:notset}")
    private String buyerPassword;
    @Value("${buyer.mnemonic:notset}")
    private String buyerMnemonic;
    @Value("${courier.password:notset}")
    private String courierPassword;
    @Value("${courier.mnemonic:notset}")
    private String courierMnemonic;

    // Credentials
    private Credentials sellerCredentials = null;
    private Credentials buyerCredentials = null;
    private Credentials courierCredentials = null;

    private BigInteger orderNumber = BigInteger.valueOf(1);
    //private BigInteger invoiceNumber = BigInteger.valueOf(1);
    private BigInteger orderPrice = BigInteger.valueOf(100000);
    private BigInteger shipmentPrice = BigInteger.valueOf(50000);
    private BigInteger price = orderPrice.add(shipmentPrice);
    private String productName = "Wine";
    private BigInteger productQuantity = BigInteger.valueOf(200);

    private Product productValidationContract = null;
    @Value("${productcontract.address:notset}")
    private String productValidationContractAddress;

    protected Map<String, Credentials> credentialMap = null;
    private int logBalanceCallCount = 0;

    @BeforeAll
    public void setupTests() throws URISyntaxException {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        // HttpLogger
        HttpLogger httpLogger = new HttpLogger();
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(httpLogger);
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Web3j
        OkHttpClient client = new OkHttpClient.Builder()
                //.callTimeout(20, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                //.addInterceptor(new LoggingInterceptor())
                .addInterceptor(interceptor)
                .build();

        //HttpService httpService = new HttpService("http://" + nodeAddress, OkHttpClient());
        //HttpService httpService = new HttpService("http://" + nodeAddress);
        //String url = String.format("http://%s:%d", this.nodeAddress, this.nodePort);
        HttpService httpService = new HttpService(this.url, client, false);
        this.web3j = Web3j.build(httpService);

        //String url = String.format("ws://%s:%d", this.nodeAddress, this.nodePort);
        //WebSocketClient webSocketClient = new WebSocketClient(new URI(url));
        //final boolean includeRawResponse = false;
        //WebSocketService webSocketService = new WebSocketService(webSocketClient, includeRawResponse);
        //this.web3j = Web3j.build(webSocketService);

        // Credentials

        // Ganache
        /*
        this.sellerCredentials = Credentials.create(this.sellerPrivateKey);
        this.buyerCredentials = Credentials.create(this.buyerPrivateKey);
        this.courierCredentials = Credentials.create(this.courierPrivateKey);
        */

        // created with MetaMask
        ///*
        this.sellerCredentials = Credentials.create(this.sellerPrivateKey);
        ////this.sellerCredentials = WalletUtils.loadBip39Credentials("password", "mnemonic");
        // created with Web3j
        this.buyerCredentials = Bip44WalletUtils.loadBip44Credentials(this.buyerPassword, this.sellerMnemonic);
        this.courierCredentials = Bip44WalletUtils.loadBip44Credentials(this.courierPassword, this.sellerMnemonic);
        //*/

        BigInteger sellerBalance = this.getBalance(this.sellerCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        logger.info("{} seller balance: {}", methodName, sellerBalance.toString());

        BigInteger transactionCount = this.getTransactionCount(this.sellerCredentials.getAddress(), DefaultBlockParameterName.PENDING);
        logger.info("{} transaction count: {}", methodName, transactionCount.toString());
        /*
        EthBlock.Block block = this.getBlockByNumber(DefaultBlockParameterName.LATEST);
        logger.info("{} latest block logs bloom: {}", methodName, block.getLogsBloom());
        String baseFee = block.getBaseFeePerGas();
        if (baseFee.startsWith("0x")) {
            baseFee = baseFee.substring(2);
        }
        BigInteger baseFeePerGas = new BigInteger(baseFee, 16);
        logger.info("{} latest block base fee per gas: {}", methodName, baseFeePerGas.toString());
        logger.info("{} latest block transactions roots: {}", methodName, block.getTransactionsRoot());
        */

        // ContractGasProvider setup
        EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        gasProvider.setDefaultGasLimit(null);   // enables call of ethGetBlockByNumber in getLatestGasLimit of EstimatedGasProvider
        //gasProvider.setGasPriceMultiplier(0.50);
        gasProvider.setGasPriceMultiplier(1.0);

        // Contract deployment
        /*
        // ProductSale.BINARY
        // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
        //String from = null
        String from = this.sellerCredentials.getAddress();
        Transaction transaction = new Transaction(from, BigInteger.ZERO, BigInteger.ZERO,
                gasProvider.getDefaultGasLimit(), null, BigInteger.ZERO, ProductSale.BINARY);
        gasProvider.setTransaction(transaction);

        //RemoteCall<ProductSale> remoteCall = ProductSale.deploy(this.web3j, this.sellerCredentials, gasProvider);
        TransactionManager transactionManager = new RawTransactionManager(this.web3j, this.sellerCredentials, ChainIdLong.MAINNET);
        RemoteCall<ProductSale> remoteCall = ProductSale.deploy(this.web3j, transactionManager, gasProvider);

        ProductSale productSaleContract = null;
        try {
            productSaleContract = remoteCall.send();
        } catch (Exception ex) {
            //System.err.println(ex.getMessage());
            logger.error(ex.getMessage());
        }

        assertNotNull(productSaleContract);
        this.productSale = productSaleContract;
        String contractAddress = productSaleContract.getContractAddress();
        assertNotNull(contractAddress);

        logger.info("product sale contract address: {}", this.productSale.getContractAddress());
        */

        // Contract load with Credentials
        this.productSale = ProductSale.load(this.contractAddress, this.web3j, this.sellerCredentials, gasProvider);

        // Contract load with TransactionManager
        /*
        PollingRawTransactionManager transactionManager = new PollingRawTransactionManager(this.web3j, this.sellerCredentials, ChainIdLong.MAINNET);
        //transactionManager.setNonce(nonce);
        //transactionManager.setGasPriceMultiplier(0.85);
        transactionManager.setGasPriceMultiplier(1);
        this.productSale = ProductSale.load(this.contractAddress, this.web3j, transactionManager, gasProvider);
        */
        //List<Credentials> credentialList = Arrays.asList(this.sellerCredentials, this.buyerCredentials, this.courierCredentials);
        this.credentialMap = Map.of(
                "seller", this.sellerCredentials,
                "buyer", this.buyerCredentials,
                "courier", this.courierCredentials);
    }

    @BeforeEach
    public void logBalance() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);
        //logger.info("logBalance");

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

    protected BigInteger getTransactionCount(String address, DefaultBlockParameterName defaultBlockParameterName) {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        Request<?, EthGetTransactionCount> getTransactionCountRequest = this.web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING);
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

    public org.web3j.protocol.core.methods.response.Transaction getTransactionByHash(String transactionHash) {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        Request<?, EthTransaction> getTransactionByHashRequest = this.web3j.ethGetTransactionByHash(transactionHash);
        EthTransaction ethTransaction = null;
        try {
            ethTransaction = getTransactionByHashRequest.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{}", ex.getMessage());
        }

        Optional<org.web3j.protocol.core.methods.response.Transaction> optionalTransaction = ethTransaction.getTransaction();
        if (!optionalTransaction.isPresent()) {
            return null;
        }

        return optionalTransaction.get();
    }

    public EthBlock.Block getBlockByNumber(DefaultBlockParameter defaultBlockParameter) {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
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

    public String deployWithCustomNonce(BigInteger gasLimit, BigInteger nonce, Credentials credentials) {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        BigInteger value = BigInteger.ZERO;
        String data = ProductSale.BINARY; //+ encoded constructor
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

    public String deployWithPolling(BigInteger gasLimit, BigInteger nonce, Credentials credentials) {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        String to = null;
        BigInteger value = BigInteger.ZERO;
        String data = ProductSale.BINARY; //+ encoded constructor
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

    @Test
    @Disabled
    public void testContractDeployWithCustomNonce() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        BigInteger gasLimit = BigInteger.valueOf(1283823L); //1283823L
        BigInteger nonce = BigInteger.valueOf(3L);
        Credentials credentials = this.sellerCredentials;

        //String transactionHash = this.deployWithCustomNonce(gasLimit, nonce, credentials);
        String transactionHash = this.deployWithPolling(gasLimit, nonce, credentials);
        assertNotNull(transactionHash);
        // TODO: remove - used with mocked API call in PollingRawTransactionManager.sendTransaction() method
        //transactionHash = this.deployWithCustomNonce(gasLimit, nonce, credentials);

        org.web3j.protocol.core.methods.response.Transaction contractCreationTransaction = this.getTransactionByHash(transactionHash);
        assertNotNull(contractCreationTransaction);
        logger.info("{} {}", methodName, contractCreationTransaction.toString());
        assertEquals(nonce, contractCreationTransaction.getNonce());
    }

    @DisplayName("shouldTestConnection")
    @Test
    //@Disabled
    public void shouldTestConnection() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //Request<?, EthAccounts> request = this.web3j.ethAccounts();
        //EthAccounts accounts = null;

        Request<?, Web3ClientVersion> request = this.web3j.web3ClientVersion();
        Web3ClientVersion clientVersion = null;

        try {
            //request.sendAsync().
            clientVersion = request.send();
            //accounts = request.send();
            //System.out.println(clientVersion.getWeb3ClientVersion());
        } catch (IOException ioex) {
            System.err.println("IOException: " + ioex.getMessage());
            ioex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }

        //assertTrue( true );
        assertNotNull(clientVersion);
        assertFalse(clientVersion.hasError());
        logger.info("{} client version: {}", methodName, clientVersion.getWeb3ClientVersion());
        assertEquals(this.web3ClientVersionDescription, clientVersion.getWeb3ClientVersion());
    }

    @Test
    @Disabled("testContractDeploy disabled")
    public void testContractDeploy() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        // eth_getTransactionReceipt
        Optional<TransactionReceipt> optTransactionReceipt = this.productSale.getTransactionReceipt();
        assertTrue(optTransactionReceipt.isPresent());

        TransactionReceipt contractDeploymentTransactionReceipt = optTransactionReceipt.get();
        assertNotNull(contractDeploymentTransactionReceipt);
        assertTrue(contractDeploymentTransactionReceipt.isStatusOK());

        BigInteger blockNumber = contractDeploymentTransactionReceipt.getBlockNumber();
        assertNotNull(blockNumber);

        logger.info("contractDeploymentTransactionReceipt block number {}", blockNumber);
        logger.info("contractDeploymentTransactionReceipt.getGasUsed: {}", contractDeploymentTransactionReceipt.getGasUsed());
        logger.info("contractDeploymentTransactionReceipt: {}", contractDeploymentTransactionReceipt.toString());
    }

    @Test
    //@Disabled("testContractLoad disabled")
    public void testContractLoad() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        // ContractGasProvider setup
        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        //gasProvider.setDefaultGasLimit(null);
        //gasProvider.setGasPriceMultiplier(1.0);

        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        // Contract load
        this.productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, this.sellerCredentials, gasProvider);
        //this.productSale.setTestMode(true);

        // eth_getCode
        ProductSale productSaleContract = this.productSale;
        boolean isContractValid = false;
        try {
            isContractValid = productSaleContract.isValid();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

        assertTrue(isContractValid);
    }

    @Test
    //@Disabled
    public void testContractOwner() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        RemoteFunctionCall<String> remoteFunctionCall = this.productSale.getOwner();
        String contractOwnerName = null;
        try {
            contractOwnerName = remoteFunctionCall.send();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            assertTrue(false);
        }

        assertEquals(this.sellerCredentials.getAddress(), contractOwnerName);
    }

    @Test
    //@Disabled("testOrder disabled")
    public void testOrder() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        if (this.productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            EstimatedGasProvider estimatedContractGasProvider = (EstimatedGasProvider)gasProvider;
            //estimatedContractGasProvider.setUseBaseFee(false); // set to false for Ganache
            estimatedContractGasProvider.setUseBaseFee(true); // set to true for Mainnet
        }
        //ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, this.buyerCredentials, gasProvider);

        Credentials credentials = this.buyerCredentials;
        credentials = this.sellerCredentials;   // test for gas estimation
        PollingRawTransactionManager transactionManager = new PollingRawTransactionManager(this.web3j, credentials, ChainIdLong.MAINNET);
        //transactionManager.setNonce(nonce);
        //transactionManager.setGasPriceMultiplier(0.85);
        transactionManager.setGasPriceMultiplier(1);
        this.productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, transactionManager, gasProvider);
        this.productSale.setTestMode(true); // test for gas estimation

        // send order
        RemoteFunctionCall<TransactionReceipt> sendOrderFunctionCall = this.productSale.sendOrder(this.productName, this.productQuantity);
        TransactionReceipt transactionReceipt = null;
        logger.info("-- sendOrder --");
        try {
            transactionReceipt = sendOrderFunctionCall.send();
        } catch (Exception ex) {
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendOrder end --");

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());

        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        BigInteger blockNumber = null;
        try {
             blockNumber = transactionReceipt.getBlockNumber();
        } catch (Exception e) {
            logger.error("exception: {}", e.getMessage());
        }
        logger.info("sendOrderTransactionReceipt block number {}", blockNumber);

        BigInteger gasUsed = null;
        try {
            gasUsed = transactionReceipt.getGasUsed();
        } catch (Exception e) {
            logger.error("exception: {}", e.getMessage());
        }
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", gasUsed);

        String transactionHash = transactionReceipt.getTransactionHash();
        Request<?, EthTransaction> request = this.web3j.ethGetTransactionByHash(transactionHash);
        EthTransaction ethTransaction = null;
        try {
            ethTransaction = request.send();
        } catch (Exception ex) {
            logger.error("exception: {}", ex.getMessage());
        }

        assertNotNull(ethTransaction);

        Optional<org.web3j.protocol.core.methods.response.Transaction> optionalTransaction = ethTransaction.getTransaction();
        assertTrue(optionalTransaction.isPresent());
        org.web3j.protocol.core.methods.response.Transaction transaction = optionalTransaction.get();
        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("transaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("transaction gas: {}", gas);

        if (this.productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("transaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // query order
        RemoteFunctionCall<Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger>> queryOrderFunctionCall = productSale.queryOrder(this.orderNumber);
        Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger> order = null;
        logger.info("-- queryOrder --");
        try {
            order = queryOrderFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exceptions: {}", ex.getMessage());
        }
        logger.info("-- queryOrder end --");

        assertNotNull(order);
        logger.info("order: {}", order.toString());
    }

    @Test
    @Disabled
    public void testOrderBuyer() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.buyerCredentials;
        //Credentials credentials = this.courierCredentials;
        ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // send order
        RemoteFunctionCall<TransactionReceipt> sendOrderFunctionCall = productSale.sendOrder(this.productName, this.productQuantity);
        TransactionReceipt transactionReceipt = null;
        logger.info("-- sendOrder --");
        try {
            transactionReceipt = sendOrderFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendOrder end --");

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());
        logger.info("sendOrderTransactionReceipt block number {}", transactionReceipt.getBlockNumber());
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", transactionReceipt.getGasUsed());
        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        // get transaction from transaction receipt
        String transactionHash = transactionReceipt.getTransactionHash();
        Request<?, EthTransaction> request = this.web3j.ethGetTransactionByHash(transactionHash);
        EthTransaction ethTransaction = null;
        try {
            ethTransaction = request.send();
        } catch (Exception ex) {
            logger.error("exception: {}", ex.getMessage());
        }

        assertNotNull(ethTransaction);

        Optional<org.web3j.protocol.core.methods.response.Transaction> optionalTransaction = ethTransaction.getTransaction();

        assertTrue(optionalTransaction.isPresent());

        org.web3j.protocol.core.methods.response.Transaction transaction = optionalTransaction.get();
        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("transaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("transaction gas: {}", gas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("transaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // query order
        RemoteFunctionCall<Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger>> queryOrderFunctionCall = productSale.queryOrder(this.orderNumber);
        Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger> order = null;
        logger.info("-- queryOrder --");
        try {
            order = queryOrderFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exceptions: {}", ex.getMessage());
        }
        logger.info("-- queryOrder end --");

        assertNotNull(order);
        logger.info("order: {}", order.toString());

        String orderBuyerAddress = order.component1();
        String orderProductName = order.component2();
        BigInteger orderQuantity = order.component3();
        assertEquals(this.buyerCredentials.getAddress(), orderBuyerAddress);
    }

    @Test
    @Disabled
    public void testOrderPrice() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.buyerCredentials;
        //Credentials credentials = this.courierCredentials;
        ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // send order from buyer
        RemoteFunctionCall<TransactionReceipt> sendOrderFunctionCall = productSale.sendOrder(this.productName, this.productQuantity);
        TransactionReceipt transactionReceipt = null;
        logger.info("-- sendOrder --");
        try {
            transactionReceipt = sendOrderFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendOrder end --");

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());
        logger.info("sendOrderTransactionReceipt block number {}", transactionReceipt.getBlockNumber());
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", transactionReceipt.getGasUsed());
        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        // get transaction from transaction receipt
        String transactionHash = transactionReceipt.getTransactionHash();
        Request<?, EthTransaction> request = this.web3j.ethGetTransactionByHash(transactionHash);
        EthTransaction ethTransaction = null;
        try {
            ethTransaction = request.send();
        } catch (Exception ex) {
            logger.error("exception: {}", ex.getMessage());
        }

        assertNotNull(ethTransaction);

        Optional<org.web3j.protocol.core.methods.response.Transaction> optionalTransaction = ethTransaction.getTransaction();

        assertTrue(optionalTransaction.isPresent());

        org.web3j.protocol.core.methods.response.Transaction transaction = optionalTransaction.get();
        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("sendOrderTransaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("sendOrderTransaction gas: {}", gas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get latest orders from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // create sendOrderFlow
        Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        // create sendOrderFlow with custom EthFilter
        //org.web3j.protocol.core.methods.request.EthFilter orderSentEventFilter = new org.web3j.protocol.core.methods.request.EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST, this.productSale.getContractAddress());
        //String topic = EventEncoder.encode(ProductSale.ORDERSENT_EVENT);
        //orderSentEventFilter.addSingleTopic(topic);
        //orderSentEventFilter.addOptionalTopics(String eventParam1, String eventParam2);
        //Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(orderSentEventFilter);

        //sendOrderFlow.map(orderSentEventResponse -> orderSentEventResponse.orderno)
                //.forEach(bigInteger -> System.out.println(bigInteger));
        /*
        sendOrderFlow.subscribe(orderSentEventResponse -> {
            logger.info("{} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.goods,
                    orderSentEventResponse.quantity);
        });
        */

        List<ProductSale.OrderSentEventResponse> orderSentEventResponseList = new ArrayList<>();
        sendOrderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(orderSentEventResponse -> {
            orderSentEventResponseList.add(orderSentEventResponse);
        });

        //orderSentEventResponseList = sendOrderFlow.timeout(1, TimeUnit.SECONDS).toList().blockingGet();
        //orderSentEventResponseList = productSale.getOrderSentEvents(transactionReceipt);

        BigInteger orderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : orderSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            orderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(orderNumber);
        logger.info("{} orderNumber: {}", methodName, orderNumber.toString());

        // send price for order from seller
        BigInteger priceType = ProductSaleTest.ORDER_PRICE_TYPE;
        RemoteFunctionCall<TransactionReceipt> sendPriceFunctionCall = productSale.sendPrice(orderNumber, this.orderPrice, priceType);
        TransactionReceipt sendOrderPriceTransactionReceipt = null;
        logger.info("-- sendPrice order--");
        try {
            sendOrderPriceTransactionReceipt = sendPriceFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendPrice order end --");

        assertNotNull(sendOrderPriceTransactionReceipt);
        assertTrue(sendOrderPriceTransactionReceipt.isStatusOK());
        logger.info("sendOrderPriceTransactionReceipt block number {}", sendOrderPriceTransactionReceipt.getBlockNumber());
        logger.info("sendOrderPriceTransactionReceipt.getGasUsed: {}", sendOrderPriceTransactionReceipt.getGasUsed());
        logger.info("sendOrderPriceTransactionReceipt: {}", sendOrderPriceTransactionReceipt.toString());

        // get transaction from transaction receipt
        String sendPriceTransactionHash = sendOrderPriceTransactionReceipt.getTransactionHash();
        Request<?, EthTransaction> sendOrderPriceTransactionRequest = this.web3j.ethGetTransactionByHash(sendPriceTransactionHash);
        EthTransaction sendOrderPriceEthTransaction = null;
        try {
            sendOrderPriceEthTransaction = sendOrderPriceTransactionRequest.send();
        } catch (Exception ex) {
            logger.error("exception: {}", ex.getMessage());
        }

        assertNotNull(sendOrderPriceEthTransaction);

        Optional<org.web3j.protocol.core.methods.response.Transaction> optionalSendOrderPriceTransaction = sendOrderPriceEthTransaction.getTransaction();
        assertTrue(optionalSendOrderPriceTransaction.isPresent());

        org.web3j.protocol.core.methods.response.Transaction sendOrderPriceTransaction = optionalSendOrderPriceTransaction.get();
        BigInteger sendOrderPriceGasPrice = sendOrderPriceTransaction.getGasPrice();
        logger.info("sendOrderPriceTransaction gas price: {}", sendOrderPriceGasPrice);
        BigInteger sendOrderPriceGas = sendOrderPriceTransaction.getGas();
        logger.info("sendOrderPriceTransaction gas: {}", sendOrderPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // query order from buyer
        gasProvider = this.productSale.getGasProvider();
        credentials = this.buyerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        RemoteFunctionCall<Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger>> queryOrderFunctionCall = productSale.queryOrder(orderNumber);
        Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger> order = null;

        logger.info("-- queryOrder --");
        try {
            order = queryOrderFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exceptions: {}", ex.getMessage());
        }
        logger.info("-- queryOrder end --");

        assertNotNull(order);
        logger.info("order: {}", order.toString());

        String orderBuyerAddress = order.component1();
        String orderProductName = order.component2();
        BigInteger orderQuantity = order.component3();
        BigInteger orderPrice = order.component4();
        assertEquals(this.orderPrice, orderPrice);
    }

    @Test
    @Disabled
    public void testShipmentPrice() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.buyerCredentials;
        //Credentials credentials = this.courierCredentials;
        ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // send order from buyer
        RemoteFunctionCall<TransactionReceipt> sendOrderFunctionCall = productSale.sendOrder(this.productName, this.productQuantity);
        TransactionReceipt transactionReceipt = null;
        logger.info("-- sendOrder --");
        try {
            transactionReceipt = sendOrderFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendOrder end --");

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());
        logger.info("sendOrderTransactionReceipt block number {}", transactionReceipt.getBlockNumber());
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", transactionReceipt.getGasUsed());
        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        // get transaction from transaction receipt
        String transactionHash = transactionReceipt.getTransactionHash();
        Request<?, EthTransaction> request = this.web3j.ethGetTransactionByHash(transactionHash);
        EthTransaction ethTransaction = null;
        try {
            ethTransaction = request.send();
        } catch (Exception ex) {
            logger.error("exception: {}", ex.getMessage());
        }

        assertNotNull(ethTransaction);

        Optional<org.web3j.protocol.core.methods.response.Transaction> optionalTransaction = ethTransaction.getTransaction();
        assertTrue(optionalTransaction.isPresent());

        org.web3j.protocol.core.methods.response.Transaction transaction = optionalTransaction.get();
        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("sendOrderTransaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("sendOrderTransaction gas: {}", gas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get latest orders from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // create sendOrderFlow
        Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        // create sendOrderFlow with custom EthFilter
        //org.web3j.protocol.core.methods.request.EthFilter orderSentEventFilter = new org.web3j.protocol.core.methods.request.EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST, this.productSale.getContractAddress());
        //String topic = EventEncoder.encode(ProductSale.ORDERSENT_EVENT);
        //orderSentEventFilter.addSingleTopic(topic);
        //orderSentEventFilter.addOptionalTopics(String eventParam1, String eventParam2);
        //Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(orderSentEventFilter);

        //sendOrderFlow.map(orderSentEventResponse -> orderSentEventResponse.orderno)
        //.forEach(bigInteger -> System.out.println(bigInteger));
        /*
        sendOrderFlow.subscribe(orderSentEventResponse -> {
            logger.info("{} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.goods,
                    orderSentEventResponse.quantity);
        });
        */

        List<ProductSale.OrderSentEventResponse> orderSentEventResponseList = new ArrayList<>();
        sendOrderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(orderSentEventResponse -> {
                    orderSentEventResponseList.add(orderSentEventResponse);
                });

        //orderSentEventResponseList = sendOrderFlow.timeout(1, TimeUnit.SECONDS).toList().blockingGet();
        //orderSentEventResponseList = productSale.getOrderSentEvents(transactionReceipt);

        BigInteger orderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : orderSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            orderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(orderNumber);
        logger.info("{} orderNumber: {}", methodName, orderNumber.toString());

        // send price for shipment from seller
        BigInteger priceType = BigInteger.TWO;
        RemoteFunctionCall<TransactionReceipt> sendPriceFunctionCall = productSale.sendPrice(orderNumber, this.shipmentPrice, priceType);
        TransactionReceipt sendShipmentPriceTransactionReceipt = null;
        logger.info("-- sendPrice shipment --");
        try {
            sendShipmentPriceTransactionReceipt = sendPriceFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendPrice shipment end --");

        assertNotNull(sendShipmentPriceTransactionReceipt);
        assertTrue(sendShipmentPriceTransactionReceipt.isStatusOK());
        logger.info("sendShipmentPriceTransactionReceipt block number {}", sendShipmentPriceTransactionReceipt.getBlockNumber());
        logger.info("sendShipmentPriceTransactionReceipt.getGasUsed: {}", sendShipmentPriceTransactionReceipt.getGasUsed());
        logger.info("sendShipmentPriceTransactionReceipt: {}", sendShipmentPriceTransactionReceipt.toString());

        // get transaction from transaction receipt
        String sendShipmentPriceTransactionHash = sendShipmentPriceTransactionReceipt.getTransactionHash();
        Request<?, EthTransaction> sendShipmentPriceTransactionRequest = this.web3j.ethGetTransactionByHash(sendShipmentPriceTransactionHash);
        EthTransaction sendShipmentPriceEthTransaction = null;
        try {
            sendShipmentPriceEthTransaction = sendShipmentPriceTransactionRequest.send();
        } catch (Exception ex) {
            logger.error("exception: {}", ex.getMessage());
        }

        assertNotNull(sendShipmentPriceEthTransaction);

        Optional<org.web3j.protocol.core.methods.response.Transaction> optionalSendShipmentPriceTransaction = sendShipmentPriceEthTransaction.getTransaction();
        assertTrue(optionalSendShipmentPriceTransaction.isPresent());

        org.web3j.protocol.core.methods.response.Transaction sendShipmentPriceTransaction = optionalSendShipmentPriceTransaction.get();
        BigInteger sendShipmentPriceGasPrice = sendShipmentPriceTransaction.getGasPrice();
        logger.info("sendShipmentPriceTransaction gas price: {}", sendShipmentPriceGasPrice);
        BigInteger sendShipmentPriceGas = sendShipmentPriceTransaction.getGas();
        logger.info("sendShipmentPriceTransaction gas: {}", sendShipmentPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendShipmentPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // query order from buyer
        gasProvider = this.productSale.getGasProvider();
        credentials = this.buyerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        RemoteFunctionCall<Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger>> queryOrderFunctionCall = productSale.queryOrder(orderNumber);
        Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger> order = null;

        logger.info("-- queryOrder shipment price--");
        try {
            order = queryOrderFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exceptions: {}", ex.getMessage());
        }
        logger.info("-- queryOrder shipment price end --");

        assertNotNull(order);
        logger.info("order: {}", order.toString());

        String orderBuyerAddress = order.component1();
        String orderProductName = order.component2();
        BigInteger orderQuantity = order.component3();
        BigInteger orderPrice = order.component4();
        BigInteger shipmentPrice = order.component6();
        assertEquals(this.shipmentPrice, shipmentPrice);
    }

    private TransactionReceipt sendOrder(ProductSale productSale, String productName, BigInteger quantity) {
        RemoteFunctionCall<TransactionReceipt> sendOrderFunctionCall = productSale.sendOrder(this.productName, this.productQuantity);
        TransactionReceipt transactionReceipt = null;
        logger.info("-- sendOrder --");
        try {
            transactionReceipt = sendOrderFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendOrder end --");
        return transactionReceipt;
    }

    private org.web3j.protocol.core.methods.response.Transaction getTransactionForReceipt(TransactionReceipt transactionReceipt) {
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

        Optional<org.web3j.protocol.core.methods.response.Transaction> optionalTransaction = ethTransaction.getTransaction();
        assertTrue(optionalTransaction.isPresent());
        if (!optionalTransaction.isPresent()) {
            return null;
        }

        org.web3j.protocol.core.methods.response.Transaction transaction = optionalTransaction.get();
        return transaction;
    }

    private TransactionReceipt sendPrice(ProductSale productSale, BigInteger orderNumber, BigInteger orderPrice, BigInteger priceType) {
        RemoteFunctionCall<TransactionReceipt> sendPriceFunctionCall = productSale.sendPrice(orderNumber, orderPrice, priceType);
        TransactionReceipt sendOrderPriceTransactionReceipt = null;
        logger.info("-- sendPrice {}--", priceType.toString());
        try {
            sendOrderPriceTransactionReceipt = sendPriceFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendPrice {} end --", priceType.toString());
        return sendOrderPriceTransactionReceipt;
    }

    private Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> queryOrder(ProductSale productSale, BigInteger orderNumber) {
        RemoteFunctionCall<Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger>> queryOrderFunctionCall = productSale.queryOrder(orderNumber);
        Tuple7<String, String, BigInteger,
                BigInteger, BigInteger, BigInteger, BigInteger> order = null;

        logger.info("-- queryOrder --");
        try {
            order = queryOrderFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exceptions: {}", ex.getMessage());
            return null;
        }
        logger.info("-- queryOrder end --");
        return order;
    }

    private TransactionReceipt sendSafePayment(ProductSale productSale, BigInteger orderNumber, BigInteger safePayment) {
        RemoteFunctionCall<TransactionReceipt> sendSafepayFunctionCall = productSale.sendSafepay(orderNumber, safePayment);
        TransactionReceipt sendSafepayTransactionReceipt = null;
        logger.info("-- sendSafepay--");
        try {
            sendSafepayTransactionReceipt = sendSafepayFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendSafepay end --");
        return sendSafepayTransactionReceipt;
    }

    @Test
    @Disabled
    public void testSafepay() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //EstimatedGasProvider estimGasProvider = (EstimatedGasProvider) gasProvider;
        //estimGasProvider.setDefaultGasLimit(null);
        //gasProvider = estimGasProvider;
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.buyerCredentials;
        //Credentials credentials = this.courierCredentials;
        ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // send order from buyer
        TransactionReceipt transactionReceipt = this.sendOrder(productSale, this.productName, this.productQuantity);

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());
        logger.info("sendOrderTransactionReceipt block number {}", transactionReceipt.getBlockNumber());
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", transactionReceipt.getGasUsed());
        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        // get transaction from transaction receipt
        org.web3j.protocol.core.methods.response.Transaction transaction = this.getTransactionForReceipt(transactionReceipt);

        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("sendOrderTransaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("sendOrderTransaction gas: {}", gas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get latest orders from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // create sendOrderFlow
        Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        // create sendOrderFlow with custom EthFilter
        //org.web3j.protocol.core.methods.request.EthFilter orderSentEventFilter = new org.web3j.protocol.core.methods.request.EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST, this.productSale.getContractAddress());
        //String topic = EventEncoder.encode(ProductSale.ORDERSENT_EVENT);
        //orderSentEventFilter.addSingleTopic(topic);
        //orderSentEventFilter.addOptionalTopics(String eventParam1, String eventParam2);
        //Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(orderSentEventFilter);

        //sendOrderFlow.map(orderSentEventResponse -> orderSentEventResponse.orderno)
        //.forEach(bigInteger -> System.out.println(bigInteger));
        /*
        sendOrderFlow.subscribe(orderSentEventResponse -> {
            logger.info("{} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.goods,
                    orderSentEventResponse.quantity);
        });
        */

        List<ProductSale.OrderSentEventResponse> orderSentEventResponseList = new ArrayList<>();
        sendOrderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(orderSentEventResponse -> {
                    orderSentEventResponseList.add(orderSentEventResponse);
                });

        //orderSentEventResponseList = sendOrderFlow.timeout(1, TimeUnit.SECONDS).toList().blockingGet();
        //orderSentEventResponseList = productSale.getOrderSentEvents(transactionReceipt);

        BigInteger orderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : orderSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            orderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(orderNumber);
        logger.info("{} orderNumber: {}", methodName, orderNumber.toString());

        // send price for order from seller
        BigInteger priceType = ProductSaleTest.ORDER_PRICE_TYPE;
        TransactionReceipt sendOrderPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.orderPrice, priceType);

        assertNotNull(sendOrderPriceTransactionReceipt);
        assertTrue(sendOrderPriceTransactionReceipt.isStatusOK());
        logger.info("sendOrderPriceTransactionReceipt block number {}", sendOrderPriceTransactionReceipt.getBlockNumber());
        logger.info("sendOrderPriceTransactionReceipt.getGasUsed: {}", sendOrderPriceTransactionReceipt.getGasUsed());
        logger.info("sendOrderPriceTransactionReceipt: {}", sendOrderPriceTransactionReceipt.toString());

        // get transaction from sendOrderPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendOrderPriceTransaction = this.getTransactionForReceipt(sendOrderPriceTransactionReceipt);

        BigInteger sendOrderPriceGasPrice = sendOrderPriceTransaction.getGasPrice();
        logger.info("sendOrderPriceTransaction gas price: {}", sendOrderPriceGasPrice);
        BigInteger sendOrderPriceGas = sendOrderPriceTransaction.getGas();
        logger.info("sendOrderPriceTransaction gas: {}", sendOrderPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // send price for shipment from seller
        BigInteger shipmentPriceType = ProductSaleTest.SHIPMENT_PRICE_TYPE;
        TransactionReceipt sendShipmentPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.shipmentPrice, shipmentPriceType);

        assertNotNull(sendShipmentPriceTransactionReceipt);
        assertTrue(sendShipmentPriceTransactionReceipt.isStatusOK());
        logger.info("sendShipmentPriceTransactionReceipt block number {}", sendShipmentPriceTransactionReceipt.getBlockNumber());
        logger.info("sendShipmentPriceTransactionReceipt.getGasUsed: {}", sendShipmentPriceTransactionReceipt.getGasUsed());
        logger.info("sendShipmentPriceTransactionReceipt: {}", sendShipmentPriceTransactionReceipt.toString());

        // get transaction from sendShipmentPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendShipmentPriceTransaction = this.getTransactionForReceipt(sendShipmentPriceTransactionReceipt);

        BigInteger sendShipmentPriceGasPrice = sendShipmentPriceTransaction.getGasPrice();
        logger.info("sendShipmentPriceTransaction gas price: {}", sendShipmentPriceGasPrice);
        BigInteger sendShipmentPriceGas = sendShipmentPriceTransaction.getGas();
        logger.info("sendShipmentPriceTransaction gas: {}", sendShipmentPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendShipmentPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get order number from buyer
        gasProvider = this.productSale.getGasProvider();
        credentials = this.buyerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // custom EthFilter
        //org.web3j.protocol.core.methods.request.EthFilter orderEventFilter = new org.web3j.protocol.core.methods.request.EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST, this.productSale.getContractAddress());
        //String topic = EventEncoder.encode(ProductSale.PRICESENT_EVENT);
        //orderSentEventFilter.addSingleTopic(topic);
        //orderSentEventFilter.addOptionalTopics(String eventParam1, String eventParam2);
        //Flowable<ProductSale.OrderSentEventResponse> orderFlow = productSale.orderSentEventFlowable(orderEventFilter);

        Flowable<ProductSale.OrderSentEventResponse> orderFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);
        //Flowable<ProductSale.OrderSentEventResponse> orderFlow = productSale.priceSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.OrderSentEventResponse> priceSentEventResponseList = new ArrayList<>();
        orderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(priceSentEventResponse -> {
                    //logger.info("priceSentEventResponse: {}", priceSentEventResponse.toString());
                    logger.info("orderSentEventResponse: {} {}", priceSentEventResponse.orderno, priceSentEventResponse.buyer);
                    priceSentEventResponseList.add(priceSentEventResponse);
                });

        assertNotEquals(0, priceSentEventResponseList.size());

        BigInteger buyerOrderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : priceSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            buyerOrderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(buyerOrderNumber);
        logger.info("{} buyerOrderNumber: {}", methodName, buyerOrderNumber.toString());

        // query order from buyer
        Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> order = this.queryOrder(productSale, buyerOrderNumber);

        assertNotNull(order);
        logger.info("order: {}", order.toString());

        String orderBuyerAddress = order.component1();
        String orderProductName = order.component2();
        BigInteger orderQuantity = order.component3();
        BigInteger orderPrice = order.component4();
        assertEquals(this.orderPrice, orderPrice);
        BigInteger shipmentPrice = order.component6();
        assertEquals(this.shipmentPrice, shipmentPrice);

        // send safe payment from buyer
        BigInteger safePayment = orderPrice.add(shipmentPrice);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();

            org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                    ProductSale.FUNC_SENDSAFEPAY,
                    Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(buyerOrderNumber)),
                    Collections.<TypeReference<?>>emptyList());
            String functionData = FunctionEncoder.encode(function);

            // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
            Transaction safepayTransaction = new Transaction(this.buyerCredentials.getAddress(), BigInteger.ZERO, BigInteger.ZERO,
                    estimatedGasProvider.getDefaultGasLimit(), this.productSale.getContractAddress(), safePayment, functionData);

            estimatedGasProvider.setTransaction(safepayTransaction);
            //estimatedGasProvider.setGasPriceMultiplier(1.1);
        }

        TransactionReceipt sendSafepayTransactionReceipt = this.sendSafePayment(productSale, orderNumber, safePayment);

        assertNotNull(sendSafepayTransactionReceipt);
        assertTrue(sendSafepayTransactionReceipt.isStatusOK());
        logger.info("sendSafepayTransactionReceipt block number {}", sendSafepayTransactionReceipt.getBlockNumber());
        logger.info("sendSafepayTransactionReceipt.getGasUsed: {}", sendSafepayTransactionReceipt.getGasUsed());
        logger.info("sendSafepayTransactionReceipt: {}", sendSafepayTransactionReceipt.toString());

        // get transaction from sendSafepay transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendSafepayTransaction = this.getTransactionForReceipt(sendSafepayTransactionReceipt);

        BigInteger sendSafepayGasPrice = sendSafepayTransaction.getGasPrice();
        logger.info("sendSafepayTransaction gas price: {}", sendSafepayGasPrice);
        BigInteger sendSafepayGas = sendSafepayTransaction.getGas();
        logger.info("sendSafepayTransaction gas: {}", sendSafepayGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendSafepayTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // --- debugging ---
        // query order from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> orderForSellerPaymentCheck = this.queryOrder(productSale, buyerOrderNumber);
        //String orderBuyerAddress = orderForSellerPaymentCheck.component1();
        //String orderProductName = orderForSellerPaymentCheck.component2();
        //BigInteger orderQuantity = orderForSellerPaymentCheck.component3();
        //BigInteger orderPrice = orderForSellerPaymentCheck.component4();
        BigInteger payment = orderForSellerPaymentCheck.component5();
        //BigInteger shipmentPrice = orderForSellerPaymentCheck.component6();
        assertNotEquals(null, payment);
        assertNotEquals(0, payment);
        logger.info("order via direct query of seller: {}", orderForSellerPaymentCheck.toString());
        // --- debugging end ---

        // get safe payments for orders from seller

        // custom EthFilter
        //org.web3j.protocol.core.methods.request.EthFilter safepaySentEventFilter = new org.web3j.protocol.core.methods.request.EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST, this.productSale.getContractAddress());
        //String topic = EventEncoder.encode(ProductSale.SAFEPAYSENT_EVENT);
        //safepaySentEventFilter.addSingleTopic(topic);
        //safepaySentEventFilter.addOptionalTopics(String eventParam1, String eventParam2);
        //Flowable<ProductSale.SafepaySentEventResponse> safepaySentFlow = productSale.safepaySentEventFlowable(safepaySentEventFilter);

        Flowable<ProductSale.SafepaySentEventResponse> safepaySentFlow = productSale.safepaySentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.SafepaySentEventResponse> safepaySentEventResponseList = new ArrayList<>();
        safepaySentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(safepaySentEventResponse -> {
                    logger.info("safepaySentEventResponse: {}", safepaySentEventResponse.toString());
                    safepaySentEventResponseList.add(safepaySentEventResponse);
                });

        assertNotEquals(0, safepaySentEventResponseList.size());

        BigInteger sellerOrderNumber = null;
        for (ProductSale.SafepaySentEventResponse safepaySentEventResponse : safepaySentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, safepaySentEventResponse.orderno,
                    safepaySentEventResponse.buyer, safepaySentEventResponse.value,
                    safepaySentEventResponse.now);
            sellerOrderNumber = safepaySentEventResponse.orderno;
        }

        assertNotNull(sellerOrderNumber);
        logger.info("{} sellerOrderNumber: {}", methodName, sellerOrderNumber.toString());

        // query order from seller
        Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> orderForSeller = this.queryOrder(productSale, sellerOrderNumber);

        assertNotNull(orderForSeller);
        logger.info("orderForSeller: {}", orderForSeller.toString());

        orderBuyerAddress = orderForSeller.component1();
        orderProductName = orderForSeller.component2();
        orderQuantity = orderForSeller.component3();
        orderPrice = orderForSeller.component4();
        BigInteger safepay = orderForSeller.component5();
        shipmentPrice = orderForSeller.component6();

        BigInteger expectedSafepay = orderPrice.add(shipmentPrice);
        assertEquals(expectedSafepay, safepay);
    }

    @Test
    @Disabled
    public void testContractBalanceForSafepay() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.buyerCredentials;
        //Credentials credentials = this.courierCredentials;
        ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // send order from buyer
        TransactionReceipt transactionReceipt = this.sendOrder(productSale, this.productName, this.productQuantity);

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());
        logger.info("sendOrderTransactionReceipt block number {}", transactionReceipt.getBlockNumber());
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", transactionReceipt.getGasUsed());
        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        // get transaction from transaction receipt
        org.web3j.protocol.core.methods.response.Transaction transaction = this.getTransactionForReceipt(transactionReceipt);

        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("sendOrderTransaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("sendOrderTransaction gas: {}", gas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get latest orders from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // create sendOrderFlow
        Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        // create sendOrderFlow with custom EthFilter
        //org.web3j.protocol.core.methods.request.EthFilter orderSentEventFilter = new org.web3j.protocol.core.methods.request.EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST, this.productSale.getContractAddress());
        //String topic = EventEncoder.encode(ProductSale.ORDERSENT_EVENT);
        //orderSentEventFilter.addSingleTopic(topic);
        //orderSentEventFilter.addOptionalTopics(String eventParam1, String eventParam2);
        //Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(orderSentEventFilter);

        //sendOrderFlow.map(orderSentEventResponse -> orderSentEventResponse.orderno)
        //.forEach(bigInteger -> System.out.println(bigInteger));
        /*
        sendOrderFlow.subscribe(orderSentEventResponse -> {
            logger.info("{} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.goods,
                    orderSentEventResponse.quantity);
        });
        */

        List<ProductSale.OrderSentEventResponse> orderSentEventResponseList = new ArrayList<>();
        sendOrderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(orderSentEventResponse -> {
                    orderSentEventResponseList.add(orderSentEventResponse);
                });

        //orderSentEventResponseList = sendOrderFlow.timeout(1, TimeUnit.SECONDS).toList().blockingGet();
        //orderSentEventResponseList = productSale.getOrderSentEvents(transactionReceipt);

        BigInteger orderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : orderSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            orderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(orderNumber);
        logger.info("{} orderNumber: {}", methodName, orderNumber.toString());

        // send price for order from seller
        BigInteger priceType = ProductSaleTest.ORDER_PRICE_TYPE;
        TransactionReceipt sendOrderPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.orderPrice, priceType);

        assertNotNull(sendOrderPriceTransactionReceipt);
        assertTrue(sendOrderPriceTransactionReceipt.isStatusOK());
        logger.info("sendOrderPriceTransactionReceipt block number {}", sendOrderPriceTransactionReceipt.getBlockNumber());
        logger.info("sendOrderPriceTransactionReceipt.getGasUsed: {}", sendOrderPriceTransactionReceipt.getGasUsed());
        logger.info("sendOrderPriceTransactionReceipt: {}", sendOrderPriceTransactionReceipt.toString());

        // get transaction from sendOrderPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendOrderPriceTransaction = this.getTransactionForReceipt(sendOrderPriceTransactionReceipt);

        BigInteger sendOrderPriceGasPrice = sendOrderPriceTransaction.getGasPrice();
        logger.info("sendOrderPriceTransaction gas price: {}", sendOrderPriceGasPrice);
        BigInteger sendOrderPriceGas = sendOrderPriceTransaction.getGas();
        logger.info("sendOrderPriceTransaction gas: {}", sendOrderPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // send price for shipment from seller
        BigInteger shipmentPriceType = ProductSaleTest.SHIPMENT_PRICE_TYPE;
        TransactionReceipt sendShipmentPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.shipmentPrice, shipmentPriceType);

        assertNotNull(sendShipmentPriceTransactionReceipt);
        assertTrue(sendShipmentPriceTransactionReceipt.isStatusOK());
        logger.info("sendShipmentPriceTransactionReceipt block number {}", sendShipmentPriceTransactionReceipt.getBlockNumber());
        logger.info("sendShipmentPriceTransactionReceipt.getGasUsed: {}", sendShipmentPriceTransactionReceipt.getGasUsed());
        logger.info("sendShipmentPriceTransactionReceipt: {}", sendShipmentPriceTransactionReceipt.toString());

        // get transaction from sendShipmentPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendShipmentPriceTransaction = this.getTransactionForReceipt(sendShipmentPriceTransactionReceipt);

        BigInteger sendShipmentPriceGasPrice = sendShipmentPriceTransaction.getGasPrice();
        logger.info("sendShipmentPriceTransaction gas price: {}", sendShipmentPriceGasPrice);
        BigInteger sendShipmentPriceGas = sendShipmentPriceTransaction.getGas();
        logger.info("sendShipmentPriceTransaction gas: {}", sendShipmentPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendShipmentPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get order number from buyer
        gasProvider = this.productSale.getGasProvider();
        credentials = this.buyerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // custom EthFilter
        //org.web3j.protocol.core.methods.request.EthFilter orderEventFilter = new org.web3j.protocol.core.methods.request.EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST, this.productSale.getContractAddress());
        //String topic = EventEncoder.encode(ProductSale.PRICESENT_EVENT);
        //orderSentEventFilter.addSingleTopic(topic);
        //orderSentEventFilter.addOptionalTopics(String eventParam1, String eventParam2);
        //Flowable<ProductSale.OrderSentEventResponse> orderFlow = productSale.orderSentEventFlowable(orderEventFilter);

        Flowable<ProductSale.PriceSentEventResponse> orderFlow = productSale.priceSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.PriceSentEventResponse> priceSentEventResponseList = new ArrayList<>();
        orderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(priceSentEventResponse -> {
                    //logger.info("priceSentEventResponse: {}", priceSentEventResponse.toString());
                    priceSentEventResponseList.add(priceSentEventResponse);
                });

        assertNotEquals(0, priceSentEventResponseList.size());

        logger.info("buyer address: {}", this.buyerCredentials.getAddress());
        BigInteger buyerOrderNumber = null;
        for (ProductSale.PriceSentEventResponse priceSentEventResponse : priceSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, priceSentEventResponse.orderno,
                    priceSentEventResponse.buyer, priceSentEventResponse.ttype,
                    priceSentEventResponse.price);
            if (priceSentEventResponse.buyer.equalsIgnoreCase(this.buyerCredentials.getAddress())) {
                buyerOrderNumber = priceSentEventResponse.orderno;
            }
        }

        assertNotNull(buyerOrderNumber);
        logger.info("{} buyerOrderNumber: {}", methodName, buyerOrderNumber.toString());

        // query order from buyer
        Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> order = this.queryOrder(productSale, buyerOrderNumber);

        assertNotNull(order);
        logger.info("order: {}", order.toString());

        String orderBuyerAddress = order.component1();
        String orderProductName = order.component2();
        BigInteger orderQuantity = order.component3();
        BigInteger orderPrice = order.component4();
        assertEquals(this.orderPrice, orderPrice);
        BigInteger shipmentPrice = order.component6();
        assertEquals(this.shipmentPrice, shipmentPrice);

        // send safe payment from buyer
        BigInteger safePayment = orderPrice.add(shipmentPrice);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();

            org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                    ProductSale.FUNC_SENDSAFEPAY,
                    Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(buyerOrderNumber)),
                    Collections.<TypeReference<?>>emptyList());
            String functionData = FunctionEncoder.encode(function);

            // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
            //estimatedGasProvider.getDefaultGasLimit()
            Transaction safepayTransaction = new Transaction(this.buyerCredentials.getAddress(), BigInteger.ZERO, BigInteger.ZERO,
                    BigInteger.ZERO, this.productSale.getContractAddress(), safePayment, functionData);

            estimatedGasProvider.setTransaction(safepayTransaction);
        }

        TransactionReceipt sendSafepayTransactionReceipt = this.sendSafePayment(productSale, orderNumber, safePayment);

        assertNotNull(sendSafepayTransactionReceipt);
        assertTrue(sendSafepayTransactionReceipt.isStatusOK());
        logger.info("sendSafepayTransactionReceipt block number {}", sendSafepayTransactionReceipt.getBlockNumber());
        logger.info("sendSafepayTransactionReceipt.getGasUsed: {}", sendSafepayTransactionReceipt.getGasUsed());
        logger.info("sendSafepayTransactionReceipt: {}", sendSafepayTransactionReceipt.toString());

        // get transaction from sendSafepay transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendSafepayTransaction = this.getTransactionForReceipt(sendSafepayTransactionReceipt);

        BigInteger sendSafepayGasPrice = sendSafepayTransaction.getGasPrice();
        logger.info("sendSafepayTransaction gas price: {}", sendSafepayGasPrice);
        BigInteger sendSafepayGas = sendSafepayTransaction.getGas();
        logger.info("sendSafepayTransaction gas: {}", sendSafepayGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendSafepayTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        Request<?, EthGetBalance> ethGetBalanceContractRequest = this.web3j.ethGetBalance(productSale.getContractAddress(), DefaultBlockParameterName.LATEST);
        EthGetBalance ethGetBalance = null;
        try {
            ethGetBalance = ethGetBalanceContractRequest.send();
        } catch (IOException ex) {
            assertNotNull(null);
        }

        BigInteger contractBalance = ethGetBalance.getBalance();

        logger.info("{} safepayment: {}", methodName, safePayment.toString());
        logger.info("{} contract balance: {}", methodName, contractBalance.toString());

        assertEquals(safePayment, contractBalance);
    }

    private TransactionReceipt sendInvoice(ProductSale productSale, BigInteger orderNumber, BigInteger deliveryDate, String courierName) {
        RemoteFunctionCall<TransactionReceipt> sendInvoiceFunctionCall = productSale.sendInvoice(orderNumber, deliveryDate, courierName);
        TransactionReceipt transactionReceipt = null;
        logger.info("-- sendInvoice --");
        try {
            transactionReceipt = sendInvoiceFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- sendInvoice end --");
        return transactionReceipt;
    }

    private Tuple4<String, BigInteger, BigInteger, String> queryInvoice(ProductSale productSale, BigInteger invoiceNumber) {
        RemoteFunctionCall<Tuple4<String, BigInteger, BigInteger, String>> queryInvoiceFunctionCall = productSale.getInvoice(invoiceNumber);
        Tuple4<String, BigInteger, BigInteger, String> invoice = null;

        logger.info("-- queryInvoice --");
        try {
            invoice = queryInvoiceFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exceptions: {}", ex.getMessage());
            return null;
        }
        logger.info("-- queryInvoice end --");
        return invoice;
    }

    @Test
    @Disabled
    public void testInvoiceNumber() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.buyerCredentials;
        //Credentials credentials = this.courierCredentials;
        ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // send order from buyer
        TransactionReceipt transactionReceipt = this.sendOrder(productSale, this.productName, this.productQuantity);

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());
        logger.info("sendOrderTransactionReceipt block number {}", transactionReceipt.getBlockNumber());
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", transactionReceipt.getGasUsed());
        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        // get transaction from transaction receipt
        org.web3j.protocol.core.methods.response.Transaction transaction = this.getTransactionForReceipt(transactionReceipt);

        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("sendOrderTransaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("sendOrderTransaction gas: {}", gas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get latest orders from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // create sendOrderFlow
        Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        // create sendOrderFlow with custom EthFilter
        //org.web3j.protocol.core.methods.request.EthFilter orderSentEventFilter = new org.web3j.protocol.core.methods.request.EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST, this.productSale.getContractAddress());
        //String topic = EventEncoder.encode(ProductSale.ORDERSENT_EVENT);
        //orderSentEventFilter.addSingleTopic(topic);
        //orderSentEventFilter.addOptionalTopics(String eventParam1, String eventParam2);
        //Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(orderSentEventFilter);

        //sendOrderFlow.map(orderSentEventResponse -> orderSentEventResponse.orderno)
        //.forEach(bigInteger -> System.out.println(bigInteger));
        /*
        sendOrderFlow.subscribe(orderSentEventResponse -> {
            logger.info("{} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.goods,
                    orderSentEventResponse.quantity);
        });
        */

        List<ProductSale.OrderSentEventResponse> orderSentEventResponseList = new ArrayList<>();
        sendOrderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(orderSentEventResponse -> {
                    orderSentEventResponseList.add(orderSentEventResponse);
                });

        //orderSentEventResponseList = sendOrderFlow.timeout(1, TimeUnit.SECONDS).toList().blockingGet();
        //orderSentEventResponseList = productSale.getOrderSentEvents(transactionReceipt);

        BigInteger orderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : orderSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            orderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(orderNumber);
        logger.info("{} orderNumber: {}", methodName, orderNumber.toString());

        // send price for order from seller
        BigInteger priceType = ProductSaleTest.ORDER_PRICE_TYPE;
        TransactionReceipt sendOrderPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.orderPrice, priceType);

        assertNotNull(sendOrderPriceTransactionReceipt);
        assertTrue(sendOrderPriceTransactionReceipt.isStatusOK());
        logger.info("sendOrderPriceTransactionReceipt block number {}", sendOrderPriceTransactionReceipt.getBlockNumber());
        logger.info("sendOrderPriceTransactionReceipt.getGasUsed: {}", sendOrderPriceTransactionReceipt.getGasUsed());
        logger.info("sendOrderPriceTransactionReceipt: {}", sendOrderPriceTransactionReceipt.toString());

        // get transaction from sendOrderPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendOrderPriceTransaction = this.getTransactionForReceipt(sendOrderPriceTransactionReceipt);

        BigInteger sendOrderPriceGasPrice = sendOrderPriceTransaction.getGasPrice();
        logger.info("sendOrderPriceTransaction gas price: {}", sendOrderPriceGasPrice);
        BigInteger sendOrderPriceGas = sendOrderPriceTransaction.getGas();
        logger.info("sendOrderPriceTransaction gas: {}", sendOrderPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // send invoice for order from seller
        TransactionReceipt sendInvoiceTransactionReceipt = this.sendInvoice(productSale, orderNumber, ProductSaleTest.DELIVERY_DATE, this.courierCredentials.getAddress());

        assertNotNull(sendInvoiceTransactionReceipt);
        assertTrue(sendInvoiceTransactionReceipt.isStatusOK());
        logger.info("sendInvoiceTransactionReceipt block number {}", sendInvoiceTransactionReceipt.getBlockNumber());
        logger.info("sendInvoiceTransactionReceipt.getGasUsed: {}", sendInvoiceTransactionReceipt.getGasUsed());
        logger.info("sendInvoiceTransactionReceipt: {}", sendInvoiceTransactionReceipt.toString());

        // get transaction from sendInvoice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendInvoiceTransaction = this.getTransactionForReceipt(sendInvoiceTransactionReceipt);

        BigInteger sendInvoiceGasPrice = sendInvoiceTransaction.getGasPrice();
        logger.info("sendInvoiceTransaction gas price: {}", sendInvoiceGasPrice);
        BigInteger sendInvoiceGas = sendInvoiceTransaction.getGas();
        logger.info("sendInvoiceTransaction gas: {}", sendInvoiceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendInvoiceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get invoice number from buyer
        gasProvider = this.productSale.getGasProvider();
        credentials = this.buyerCredentials;
        //credentials = this.courierCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        Flowable<ProductSale.InvoiceSentEventResponse> invoiceSentFlow = productSale.invoiceSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.InvoiceSentEventResponse> invoiceSentEventResponseList = new ArrayList<>();
        invoiceSentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(invoiceSentEventResponse -> {
                    //logger.info("invoiceSentEventResponse: {}", invoiceSentEventResponse.toString());
                    invoiceSentEventResponseList.add(invoiceSentEventResponse);
                });

        assertNotEquals(0, invoiceSentEventResponseList.size());

        logger.info("buyer address: {}", this.buyerCredentials.getAddress());
        BigInteger buyerInvoiceNumber = null;
        for (ProductSale.InvoiceSentEventResponse invoiceSentEventResponse : invoiceSentEventResponseList) {
            //String courierName = new String(invoiceSentEventResponse.courier, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, invoiceSentEventResponse.invoiceno,
                    invoiceSentEventResponse.orderno, invoiceSentEventResponse.buyer,
                    invoiceSentEventResponse.courier, invoiceSentEventResponse.deliveryDate);
            if (invoiceSentEventResponse.buyer.equalsIgnoreCase(this.buyerCredentials.getAddress())) {
                buyerInvoiceNumber = invoiceSentEventResponse.invoiceno;
            }
        }

        assertNotNull(buyerInvoiceNumber);
        logger.info("{} buyerInvoiceNumber: {}", methodName, buyerInvoiceNumber.toString());

        // query invoice from buyer
        Tuple4<String, BigInteger, BigInteger, String> invoice = this.queryInvoice(productSale, buyerInvoiceNumber);
        assertNotNull(invoice);

        logger.info("invoice: {}", invoice.toString());

        String buyerAddress = invoice.component1();
        assertEquals(this.buyerCredentials.getAddress(), buyerAddress);

        BigInteger invoiceOrderNumber = invoice.component2();
        assertEquals(orderNumber, invoiceOrderNumber);

        BigInteger deliveryDate = invoice.component3();
        assertEquals(ProductSaleTest.DELIVERY_DATE, deliveryDate);

        String courierName = invoice.component4();
        assertEquals(this.courierCredentials.getAddress(), courierName);
    }

    /**
     * Method ProductSale.delivery(invoiceNumber, actualDeliveryDate) is public payable.
     * A wrong msg.sender will result in an inexecutable transaction.
     * @param productSale
     * @param invoiceNumber
     * @param actualDeliveryDate
     * @return
     */
    private TransactionReceipt delivery(ProductSale productSale, BigInteger invoiceNumber, BigInteger actualDeliveryDate) {
        RemoteFunctionCall<TransactionReceipt> deliveryFunctionCall = productSale.delivery(invoiceNumber, actualDeliveryDate);
        TransactionReceipt transactionReceipt = null;
        logger.info("-- delivery --");
        try {
            transactionReceipt = deliveryFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("exception: {}", ex.getMessage());
        }
        logger.info("-- delivery end --");
        return transactionReceipt;
    }

    private BigInteger getBalance(String address, DefaultBlockParameterName defaultBlockParameterName) {
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

    @Test
    @Disabled
    public void testContractBalanceForDelivery() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.buyerCredentials;
        ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // send order from buyer
        TransactionReceipt transactionReceipt = this.sendOrder(productSale, this.productName, this.productQuantity);

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());
        logger.info("sendOrderTransactionReceipt block number {}", transactionReceipt.getBlockNumber());
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", transactionReceipt.getGasUsed());
        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        // get transaction from transaction receipt
        org.web3j.protocol.core.methods.response.Transaction transaction = this.getTransactionForReceipt(transactionReceipt);

        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("sendOrderTransaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("sendOrderTransaction gas: {}", gas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get latest orders from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // create orderSentFlow
        Flowable<ProductSale.OrderSentEventResponse> orderSentFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.OrderSentEventResponse> orderSentEventResponseList = new ArrayList<>();
        orderSentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(orderSentEventResponse -> {
                    orderSentEventResponseList.add(orderSentEventResponse);
                });

        BigInteger orderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : orderSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            orderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(orderNumber);
        logger.info("{} orderNumber: {}", methodName, orderNumber.toString());

        // send price for order from seller
        BigInteger priceType = ProductSaleTest.ORDER_PRICE_TYPE;
        TransactionReceipt sendOrderPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.orderPrice, priceType);

        assertNotNull(sendOrderPriceTransactionReceipt);
        assertTrue(sendOrderPriceTransactionReceipt.isStatusOK());
        logger.info("sendOrderPriceTransactionReceipt block number {}", sendOrderPriceTransactionReceipt.getBlockNumber());
        logger.info("sendOrderPriceTransactionReceipt.getGasUsed: {}", sendOrderPriceTransactionReceipt.getGasUsed());
        logger.info("sendOrderPriceTransactionReceipt: {}", sendOrderPriceTransactionReceipt.toString());

        // get transaction from sendOrderPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendOrderPriceTransaction = this.getTransactionForReceipt(sendOrderPriceTransactionReceipt);

        BigInteger sendOrderPriceGasPrice = sendOrderPriceTransaction.getGasPrice();
        logger.info("sendOrderPriceTransaction gas price: {}", sendOrderPriceGasPrice);
        BigInteger sendOrderPriceGas = sendOrderPriceTransaction.getGas();
        logger.info("sendOrderPriceTransaction gas: {}", sendOrderPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // send price for shipment from seller
        BigInteger shipmentPriceType = ProductSaleTest.SHIPMENT_PRICE_TYPE;
        TransactionReceipt sendShipmentPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.shipmentPrice, shipmentPriceType);

        assertNotNull(sendShipmentPriceTransactionReceipt);
        assertTrue(sendShipmentPriceTransactionReceipt.isStatusOK());
        logger.info("sendShipmentPriceTransactionReceipt block number {}", sendShipmentPriceTransactionReceipt.getBlockNumber());
        logger.info("sendShipmentPriceTransactionReceipt.getGasUsed: {}", sendShipmentPriceTransactionReceipt.getGasUsed());
        logger.info("sendShipmentPriceTransactionReceipt: {}", sendShipmentPriceTransactionReceipt.toString());

        // get transaction from sendShipmentPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendShipmentPriceTransaction = this.getTransactionForReceipt(sendShipmentPriceTransactionReceipt);

        BigInteger sendShipmentPriceGasPrice = sendShipmentPriceTransaction.getGasPrice();
        logger.info("sendShipmentPriceTransaction gas price: {}", sendShipmentPriceGasPrice);
        BigInteger sendShipmentPriceGas = sendShipmentPriceTransaction.getGas();
        logger.info("sendShipmentPriceTransaction gas: {}", sendShipmentPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendShipmentPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get order number from buyer
        gasProvider = this.productSale.getGasProvider();
        credentials = this.buyerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        Flowable<ProductSale.PriceSentEventResponse> priceSentFlow = productSale.priceSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.PriceSentEventResponse> priceSentEventResponseList = new ArrayList<>();
        priceSentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(priceSentEventResponse -> {
                    //logger.info("priceSentEventResponse: {}", priceSentEventResponse.toString());
                    priceSentEventResponseList.add(priceSentEventResponse);
                });

        assertNotEquals(0, priceSentEventResponseList.size());

        logger.info("buyer address: {}", this.buyerCredentials.getAddress());
        BigInteger buyerOrderNumber = null;
        for (ProductSale.PriceSentEventResponse priceSentEventResponse : priceSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, priceSentEventResponse.orderno,
                    priceSentEventResponse.buyer, priceSentEventResponse.ttype,
                    priceSentEventResponse.price);
            if (priceSentEventResponse.buyer.equalsIgnoreCase(this.buyerCredentials.getAddress())) {
                buyerOrderNumber = priceSentEventResponse.orderno;
            }
        }

        assertNotNull(buyerOrderNumber);
        logger.info("{} buyerOrderNumber: {}", methodName, buyerOrderNumber.toString());

        // query order from buyer
        Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> order = this.queryOrder(productSale, buyerOrderNumber);

        assertNotNull(order);
        logger.info("order: {}", order.toString());

        String orderBuyerAddress = order.component1();
        String orderProductName = order.component2();
        BigInteger orderQuantity = order.component3();
        BigInteger orderPrice = order.component4();
        assertEquals(this.orderPrice, orderPrice);
        BigInteger shipmentPrice = order.component6();
        assertEquals(this.shipmentPrice, shipmentPrice);

        // send safe payment from buyer
        BigInteger safePayment = orderPrice.add(shipmentPrice);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            //BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();

            org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                    ProductSale.FUNC_SENDSAFEPAY,
                    Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(buyerOrderNumber)),
                    Collections.<TypeReference<?>>emptyList());
            String functionData = FunctionEncoder.encode(function);

            // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
            //estimatedGasProvider.getDefaultGasLimit()
            Transaction safepayTransaction = new Transaction(this.buyerCredentials.getAddress(), BigInteger.ZERO, BigInteger.ZERO,
                    BigInteger.ZERO, this.productSale.getContractAddress(), safePayment, functionData);

            estimatedGasProvider.setTransaction(safepayTransaction);
        }

        TransactionReceipt sendSafepayTransactionReceipt = this.sendSafePayment(productSale, orderNumber, safePayment);

        assertNotNull(sendSafepayTransactionReceipt);
        assertTrue(sendSafepayTransactionReceipt.isStatusOK());
        logger.info("sendSafepayTransactionReceipt block number {}", sendSafepayTransactionReceipt.getBlockNumber());
        logger.info("sendSafepayTransactionReceipt.getGasUsed: {}", sendSafepayTransactionReceipt.getGasUsed());
        logger.info("sendSafepayTransactionReceipt: {}", sendSafepayTransactionReceipt.toString());

        // get transaction from sendSafepay transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendSafepayTransaction = this.getTransactionForReceipt(sendSafepayTransactionReceipt);

        BigInteger sendSafepayGasPrice = sendSafepayTransaction.getGasPrice();
        logger.info("sendSafepayTransaction gas price: {}", sendSafepayGasPrice);
        BigInteger sendSafepayGas = sendSafepayTransaction.getGas();
        logger.info("sendSafepayTransaction gas: {}", sendSafepayGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendSafepayTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        BigInteger contractBalance = this.getBalance(productSale.getContractAddress(), DefaultBlockParameterName.LATEST);

        assertNotNull(contractBalance);

        logger.info("{} safepayment: {}", methodName, safePayment.toString());
        logger.info("{} contract balance: {}", methodName, contractBalance.toString());

        assertEquals(safePayment, contractBalance);

        // send invoice for order from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        TransactionReceipt sendInvoiceTransactionReceipt = this.sendInvoice(productSale, orderNumber, ProductSaleTest.DELIVERY_DATE, this.courierCredentials.getAddress());

        assertNotNull(sendInvoiceTransactionReceipt);
        assertTrue(sendInvoiceTransactionReceipt.isStatusOK());
        logger.info("sendInvoiceTransactionReceipt block number {}", sendInvoiceTransactionReceipt.getBlockNumber());
        logger.info("sendInvoiceTransactionReceipt.getGasUsed: {}", sendInvoiceTransactionReceipt.getGasUsed());
        logger.info("sendInvoiceTransactionReceipt: {}", sendInvoiceTransactionReceipt.toString());

        // get transaction from sendInvoice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendInvoiceTransaction = this.getTransactionForReceipt(sendInvoiceTransactionReceipt);

        BigInteger sendInvoiceGasPrice = sendInvoiceTransaction.getGasPrice();
        logger.info("sendInvoiceTransaction gas price: {}", sendInvoiceGasPrice);
        BigInteger sendInvoiceGas = sendInvoiceTransaction.getGas();
        logger.info("sendInvoiceTransaction gas: {}", sendInvoiceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendInvoiceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get invoice for order from courier
        gasProvider = this.productSale.getGasProvider();
        credentials = this.courierCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        Flowable<ProductSale.InvoiceSentEventResponse> invoiceSentFlow = productSale.invoiceSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.InvoiceSentEventResponse> invoiceSentEventResponseList = new ArrayList<>();
        invoiceSentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(invoiceSentEventResponse -> {
                    //logger.info("invoiceSentEventResponse: {}", invoiceSentEventResponse.toString());
                    invoiceSentEventResponseList.add(invoiceSentEventResponse);
                });

        assertNotEquals(0, invoiceSentEventResponseList.size());

        logger.info("courier address: {}", this.courierCredentials.getAddress());
        BigInteger courierInvoiceNumber = null;
        for (ProductSale.InvoiceSentEventResponse invoiceSentEventResponse : invoiceSentEventResponseList) {
            //String courierName = new String(invoiceSentEventResponse.courier, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, invoiceSentEventResponse.invoiceno,
                    invoiceSentEventResponse.orderno, invoiceSentEventResponse.buyer,
                    invoiceSentEventResponse.courier, invoiceSentEventResponse.deliveryDate);
            if (invoiceSentEventResponse.courier.equalsIgnoreCase(this.courierCredentials.getAddress())) {
                courierInvoiceNumber = invoiceSentEventResponse.invoiceno;
            }
        }

        assertNotNull(courierInvoiceNumber);
        logger.info("{} courierInvoiceNumber: {}", methodName, courierInvoiceNumber.toString());

        // query invoice from courier
        Tuple4<String, BigInteger, BigInteger, String> invoice = this.queryInvoice(productSale, courierInvoiceNumber);
        assertNotNull(invoice);

        logger.info("invoice: {}", invoice.toString());

        String buyerAddress = invoice.component1();
        assertEquals(this.buyerCredentials.getAddress(), buyerAddress);

        BigInteger invoiceOrderNumber = invoice.component2();
        //assertEquals(orderNumber, invoiceOrderNumber);
        assertEquals(buyerOrderNumber, invoiceOrderNumber);

        BigInteger deliveryDate = invoice.component3();
        assertEquals(ProductSaleTest.DELIVERY_DATE, deliveryDate);

        String courierName = invoice.component4();
        assertEquals(this.courierCredentials.getAddress(), courierName);

        // get contract balance before delivery
        BigInteger contractBalanceBeforeDelivery = this.getBalance(productSale.getContractAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(contractBalanceBeforeDelivery);
        logger.info("{} contract balance before delivery: {}", methodName, contractBalanceBeforeDelivery.toString());

        // get seller balance before delivery
        BigInteger sellerBalanceBeforeDelivery = this.getBalance(this.sellerCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(sellerBalanceBeforeDelivery);
        logger.info("{} seller balance before delivery: {}", methodName, sellerBalanceBeforeDelivery.toString());

        // get courier balance before delivery
        BigInteger courierBalanceBeforeDelivery = this.getBalance(this.courierCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(courierBalanceBeforeDelivery);
        logger.info("{} courier balance before delivery: {}", methodName, courierBalanceBeforeDelivery.toString());

        // send delivery from courier
        TransactionReceipt deliveryTransactionReceipt = this.delivery(productSale, courierInvoiceNumber, ProductSaleTest.DELIVERY_DATE);
        assertNotNull(deliveryTransactionReceipt);
        assertTrue(deliveryTransactionReceipt.isStatusOK());
        logger.info("deliveryTransactionReceipt block number {}", deliveryTransactionReceipt.getBlockNumber());
        logger.info("deliveryTransactionReceipt.getGasUsed: {}", deliveryTransactionReceipt.getGasUsed());
        logger.info("deliveryTransactionReceipt: {}", deliveryTransactionReceipt.toString());

        // get transaction from delivery transaction receipt
        org.web3j.protocol.core.methods.response.Transaction deliveryTransaction = this.getTransactionForReceipt(deliveryTransactionReceipt);

        BigInteger deliveryGasPrice = deliveryTransaction.getGasPrice();
        logger.info("deliveryTransaction gas price: {}", deliveryGasPrice);
        BigInteger deliveryGas = deliveryTransaction.getGas();
        logger.info("deliveryTransaction gas: {}", deliveryGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("deliveryTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get contract balance after delivery
        BigInteger contractBalanceAfterDelivery = this.getBalance(productSale.getContractAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(contractBalanceAfterDelivery);
        logger.info("{} contract balance after delivery: {}", methodName, contractBalanceAfterDelivery.toString());

        BigInteger contractBalanceDifference = contractBalanceBeforeDelivery.subtract(contractBalanceAfterDelivery);
        logger.info("{} safe payment value: {}", methodName, safePayment.toString());
        logger.info("{} contract balance difference: {}", methodName, contractBalanceDifference);

        assertEquals(safePayment, contractBalanceDifference);

        // get seller balance after delivery
        BigInteger sellerBalanceAfterDelivery = this.getBalance(this.sellerCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(sellerBalanceAfterDelivery);
        logger.info("{} seller balance after delivery: {}", methodName, sellerBalanceAfterDelivery.toString());

        BigInteger sellerBalanceDifference = sellerBalanceAfterDelivery.subtract(sellerBalanceBeforeDelivery);
        logger.info("{} order price: {}", methodName, orderPrice.toString());
        logger.info("{} seller balance difference: {}", methodName, sellerBalanceDifference.toString());

        assertEquals(orderPrice, sellerBalanceDifference);

        // get courier balance after delivery
        BigInteger courierBalanceAfterDelivery = this.getBalance(this.courierCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(courierBalanceAfterDelivery);
        logger.info("{} courier balance after delivery: {}", methodName, courierBalanceAfterDelivery.toString());
        logger.info("{} courier balance before deliver: {}", methodName, courierBalanceBeforeDelivery.toString());

        BigInteger deliveryTransactionFee = deliveryGas.multiply(deliveryGasPrice);
        logger.info("{} delivery transaction fee: {}", methodName, deliveryTransactionFee.toString());

        BigInteger courierBalanceDifference = BigInteger.ZERO;
        if (courierBalanceAfterDelivery.compareTo(courierBalanceBeforeDelivery) >= 0) {
            courierBalanceDifference = courierBalanceBeforeDelivery.add(deliveryTransactionFee);
            courierBalanceDifference = courierBalanceDifference.subtract(courierBalanceAfterDelivery);
        } else {
            logger.info("courierBalanceAfterDelivery < courierBalanceBeforeDelivery");
            // courierBalanceAfterDelivery < courierBalanceBeforeDelivery
            courierBalanceDifference = courierBalanceAfterDelivery.add(deliveryTransactionFee);
            courierBalanceDifference = courierBalanceDifference.subtract(courierBalanceBeforeDelivery);
        }

        logger.info("{} shipment price: {}", methodName, shipmentPrice.toString());
        logger.info("{} courier balance difference: {}", methodName, courierBalanceDifference.toString());

        assertEquals(shipmentPrice, courierBalanceDifference);
    }

    @Test
    //@Disabled
    public void testValidationContractAddress() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        // ContractGasProvider setup
        EstimatedGasProvider estimatedGasProvider = new EstimatedGasProvider(this.web3j);
        //estimatedGasProvider.setDefaultGasLimit(null);
        //estimatedGasProvider.setGasPriceMultiplier(1.1);

        if (this.productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)this.productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
        }

        // Contract deployment
        /*
        // Product.BINARY
        // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
        Transaction validationContractDeploymentTransaction = new Transaction(null, BigInteger.ZERO, BigInteger.ZERO,
                estimatedGasProvider.getDefaultGasLimit(), null, BigInteger.ZERO, Product.BINARY);
        estimatedGasProvider.setTransaction(validationContractDeploymentTransaction);

        RemoteCall<Product> remoteCall = Product.deploy(this.web3j, this.sellerCredentials, estimatedGasProvider);
        Product productValidationContract = null;
        try {
            productValidationContract = remoteCall.send();
        } catch (Exception ex) {
            //System.err.println(ex.getMessage());
            //ex.printStackTrace();
            logger.error(ex.getMessage());
        }

        assertNotNull(productValidationContract);
        this.productValidationContract = productValidationContract;

        String newValidationContractAddress = productValidationContract.getContractAddress();
        assertNotNull(newValidationContractAddress);
        this.productValidationContractAddress = newValidationContractAddress;

        logger.info("product validation contract address: {}", this.productValidationContract.getContractAddress());

        Optional<TransactionReceipt> optTransactionReceipt = this.productValidationContract.getTransactionReceipt();
        assertTrue(optTransactionReceipt.isPresent());

        TransactionReceipt validationContractDeploymentTransactionReceipt = optTransactionReceipt.get();
        assertNotNull(validationContractDeploymentTransactionReceipt);
        assertTrue(validationContractDeploymentTransactionReceipt.isStatusOK());

        BigInteger blockNumber = validationContractDeploymentTransactionReceipt.getBlockNumber();
        assertNotNull(blockNumber);

        logger.info("validationContractDeploymentTransactionReceipt block number {}", blockNumber);
        logger.info("validationContractDeploymentTransactionReceipt.getGasUsed: {}", validationContractDeploymentTransactionReceipt.getGasUsed());
        logger.info("validationContractDeploymentTransactionReceipt: {}", validationContractDeploymentTransactionReceipt.toString());
        */

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        //gasProvider.setDefaultGasLimit(null);
        //gasProvider.setGasPriceMultiplier(1.1);

        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));

        Credentials credentials = this.sellerCredentials;
        ////Credentials credentials = this.courierCredentials;
        //ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        ///*
        // test for gas estimation
        String newValidationContractAddress = this.productSale.getContractAddress();

        if (gasProvider.getClass().equals(EstimatedGasProvider.class)) {
            EstimatedGasProvider estimatedContractGasProvider = (EstimatedGasProvider)gasProvider;
            //estimatedContractGasProvider.setUseBaseFee(false); // set to false for Ganache
            estimatedContractGasProvider.setUseBaseFee(true); // set to true for Mainnet
            gasProvider = estimatedContractGasProvider;
        }

        PollingRawTransactionManager transactionManager = new PollingRawTransactionManager(this.web3j, credentials, ChainIdLong.MAINNET);
        //transactionManager.setNonce(nonce);
        transactionManager.setGasPriceMultiplier(1);
        this.productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, transactionManager, gasProvider);
        this.productSale.setTestMode(true); // test for gas estimation
        //*/

        // set validation contract address
        //RemoteFunctionCall<TransactionReceipt> setRemoteFunctionCall = productSale.setProductValidationContractAddress(this.productValidationContractAddress);
        RemoteFunctionCall<TransactionReceipt> setValidationContractRemoteFunctionCall = productSale.setProductValidationContractAddress(newValidationContractAddress);
        TransactionReceipt setValidationContractTransactionReceipt = null;

        try {
            setValidationContractTransactionReceipt = setValidationContractRemoteFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            assertNotNull(null);
        }

        assertTrue(setValidationContractTransactionReceipt.isStatusOK());
        logger.info("setValidationContractTransactionReceipt block number {}", setValidationContractTransactionReceipt.getBlockNumber());
        logger.info("setValidationContractTransactionReceipt.getGasUsed: {}", setValidationContractTransactionReceipt.getGasUsed());
        logger.info("setValidationContractTransactionReceipt: {}", setValidationContractTransactionReceipt.toString());

        // get transaction from set validation contract transaction receipt
        org.web3j.protocol.core.methods.response.Transaction setValidationContractTransaction = this.getTransactionForReceipt(setValidationContractTransactionReceipt);

        BigInteger setValidationContractGasPrice = setValidationContractTransaction.getGasPrice();
        logger.info("setValidationContractGasPrice gas price: {}", setValidationContractGasPrice);
        BigInteger setValidationContractGas = setValidationContractTransaction.getGas();
        logger.info("setValidationContractGasPrice gas: {}", setValidationContractGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("setValidationContractTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get validation contract address
        RemoteFunctionCall<String> getValidationContractRemoteFunctionCall = productSale.getProductValidationContractAddress();
        String productValidationContractAddress = null;

        try {
            productValidationContractAddress = getValidationContractRemoteFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            assertNotNull(null);
        }

        //logger.info("{} product validation contract address: {}", methodName, this.productValidationContractAddress);
        logger.info("{} new product validation contract address: {}", methodName, newValidationContractAddress);
        logger.info("{} set and returned product validation contract address: {}", methodName, productValidationContractAddress);

        //assertEquals(this.productValidationContractAddress, productValidationContractAddress);
        assertEquals(newValidationContractAddress, productValidationContractAddress);
    }

    @Test
    @Disabled
    public void testProductSaleAndDeliveryAndValidation() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        // Validation contract deployment
        ///*
        // ContractGasProvider setup
        EstimatedGasProvider estimatedGasProvider = new EstimatedGasProvider(this.web3j);
        //estimatedGasProvider.setDefaultGasLimit(null);
        //estimatedGasProvider.setGasPriceMultiplier(1.1);

        if (this.productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)this.productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
        }

        // Product.BINARY
        // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
        Transaction validationContractDeploymentTransaction = new Transaction(null, BigInteger.ZERO, BigInteger.ZERO,
                estimatedGasProvider.getDefaultGasLimit(), null, BigInteger.ZERO, Product.BINARY);
        estimatedGasProvider.setTransaction(validationContractDeploymentTransaction);

        RemoteCall<Product> remoteCall = Product.deploy(this.web3j, this.sellerCredentials, estimatedGasProvider);
        Product productValidationContract = null;
        try {
            productValidationContract = remoteCall.send();
        } catch (Exception ex) {
            //System.err.println(ex.getMessage());
            //ex.printStackTrace();
            logger.error(ex.getMessage());
        }

        assertNotNull(productValidationContract);
        this.productValidationContract = productValidationContract;

        String newValidationContractAddress = productValidationContract.getContractAddress();
        assertNotNull(newValidationContractAddress);
        this.productValidationContractAddress = newValidationContractAddress;

        logger.info("product validation contract address: {}", this.productValidationContract.getContractAddress());

        Optional<TransactionReceipt> optTransactionReceipt = this.productValidationContract.getTransactionReceipt();
        assertTrue(optTransactionReceipt.isPresent());

        TransactionReceipt validationContractDeploymentTransactionReceipt = optTransactionReceipt.get();
        assertNotNull(validationContractDeploymentTransactionReceipt);
        assertTrue(validationContractDeploymentTransactionReceipt.isStatusOK());

        BigInteger blockNumber = validationContractDeploymentTransactionReceipt.getBlockNumber();
        assertNotNull(blockNumber);

        logger.info("validationContractDeploymentTransactionReceipt block number {}", blockNumber);
        logger.info("validationContractDeploymentTransactionReceipt.getGasUsed: {}", validationContractDeploymentTransactionReceipt.getGasUsed());
        logger.info("validationContractDeploymentTransactionReceipt: {}", validationContractDeploymentTransactionReceipt.toString());
        //*/

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.sellerCredentials;
        ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // set validation contract address

        //RemoteFunctionCall<TransactionReceipt> setRemoteFunctionCall = productSale.setProductValidationContractAddress(this.productValidationContractAddress);
        RemoteFunctionCall<TransactionReceipt> setValidationContractRemoteFunctionCall = productSale.setProductValidationContractAddress(newValidationContractAddress);
        TransactionReceipt setValidationContractTransactionReceipt = null;

        try {
            setValidationContractTransactionReceipt = setValidationContractRemoteFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            assertNotNull(null);
        }

        assertTrue(setValidationContractTransactionReceipt.isStatusOK());
        logger.info("setValidationContractTransactionReceipt block number {}", setValidationContractTransactionReceipt.getBlockNumber());
        logger.info("setValidationContractTransactionReceipt.getGasUsed: {}", setValidationContractTransactionReceipt.getGasUsed());
        logger.info("setValidationContractTransactionReceipt: {}", setValidationContractTransactionReceipt.toString());

        // get transaction from set validation contract transaction receipt
        org.web3j.protocol.core.methods.response.Transaction setValidationContractTransaction = this.getTransactionForReceipt(setValidationContractTransactionReceipt);

        BigInteger setValidationContractGasPrice = setValidationContractTransaction.getGasPrice();
        logger.info("setValidationContractGasPrice gas price: {}", setValidationContractGasPrice);
        BigInteger setValidationContractGas = setValidationContractTransaction.getGas();
        logger.info("setValidationContractGasPrice gas: {}", setValidationContractGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("setValidationContractTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get validation contract address
        RemoteFunctionCall<String> getValidationContractRemoteFunctionCall = productSale.getProductValidationContractAddress();
        String productValidationContractAddress = null;

        try {
            productValidationContractAddress = getValidationContractRemoteFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            assertNotNull(null);
        }

        //logger.info("{} product validation contract address: {}", methodName, this.productValidationContractAddress);
        logger.info("{} new product validation contract address: {}", methodName, newValidationContractAddress);
        logger.info("{} set and returned product validation contract address: {}", methodName, productValidationContractAddress);

        //assertEquals(this.productValidationContractAddress, productValidationContractAddress);
        assertEquals(newValidationContractAddress, productValidationContractAddress);

        // set sale contract address

        RemoteFunctionCall<TransactionReceipt> setSaleContractRemoteFunctionCall = productValidationContract.setProductSaleContractAddress(productSale.getContractAddress());
        TransactionReceipt setSaleContractTransactionReceipt = null;

        try {
            setSaleContractTransactionReceipt = setSaleContractRemoteFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            assertNotNull(null);
        }

        assertTrue(setSaleContractTransactionReceipt.isStatusOK());
        logger.info("setSaleContractTransactionReceipt block number {}", setSaleContractTransactionReceipt.getBlockNumber());
        logger.info("setSaleContractTransactionReceipt.getGasUsed: {}", setSaleContractTransactionReceipt.getGasUsed());
        logger.info("setSaleContractTransactionReceipt: {}", setSaleContractTransactionReceipt.toString());

        // get transaction from set sale contract transaction receipt (setSaleContractTransactionReceipt)
        org.web3j.protocol.core.methods.response.Transaction setSaleContractTransaction = this.getTransactionForReceipt(setSaleContractTransactionReceipt);

        BigInteger setSaleContractGasPrice = setSaleContractTransaction.getGasPrice();
        logger.info("setSaleContractTransaction gas price: {}", setSaleContractGasPrice);
        BigInteger setSaleContractGas = setSaleContractTransaction.getGas();
        logger.info("setSaleContractTransaction gas: {}", setSaleContractGas);

        if (productValidationContract.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productValidationContract.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("setSaleContractTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get sale contract address
        RemoteFunctionCall<String> getSaleContractRemoteFunctionCall = productValidationContract.getProductSaleContractAddress();
        String productSaleContractAddress = null;

        try {
            productSaleContractAddress = getSaleContractRemoteFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            assertNotNull(null);
        }

        logger.info("{} product sale contract address: {}", methodName, productSale.getContractAddress());
        logger.info("{} set and returned product sale contract address: {}", methodName, productSaleContractAddress);

        assertEquals(productSale.getContractAddress(), productSaleContractAddress);

        // testContractBalanceForDelivery
        // send order from buyer

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        gasProvider = this.productSale.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        credentials = this.buyerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        TransactionReceipt transactionReceipt = this.sendOrder(productSale, this.productName, this.productQuantity);

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());
        logger.info("sendOrderTransactionReceipt block number {}", transactionReceipt.getBlockNumber());
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", transactionReceipt.getGasUsed());
        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        // get transaction from transaction receipt
        org.web3j.protocol.core.methods.response.Transaction transaction = this.getTransactionForReceipt(transactionReceipt);

        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("sendOrderTransaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("sendOrderTransaction gas: {}", gas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get latest orders from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // create orderSentFlow
        Flowable<ProductSale.OrderSentEventResponse> orderSentFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.OrderSentEventResponse> orderSentEventResponseList = new ArrayList<>();
        orderSentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(orderSentEventResponse -> {
                    orderSentEventResponseList.add(orderSentEventResponse);
                });

        BigInteger orderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : orderSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            orderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(orderNumber);
        logger.info("{} orderNumber: {}", methodName, orderNumber.toString());

        // send price for order from seller
        BigInteger priceType = ProductSaleTest.ORDER_PRICE_TYPE;
        TransactionReceipt sendOrderPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.orderPrice, priceType);

        assertNotNull(sendOrderPriceTransactionReceipt);
        assertTrue(sendOrderPriceTransactionReceipt.isStatusOK());
        logger.info("sendOrderPriceTransactionReceipt block number {}", sendOrderPriceTransactionReceipt.getBlockNumber());
        logger.info("sendOrderPriceTransactionReceipt.getGasUsed: {}", sendOrderPriceTransactionReceipt.getGasUsed());
        logger.info("sendOrderPriceTransactionReceipt: {}", sendOrderPriceTransactionReceipt.toString());

        // get transaction from sendOrderPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendOrderPriceTransaction = this.getTransactionForReceipt(sendOrderPriceTransactionReceipt);

        BigInteger sendOrderPriceGasPrice = sendOrderPriceTransaction.getGasPrice();
        logger.info("sendOrderPriceTransaction gas price: {}", sendOrderPriceGasPrice);
        BigInteger sendOrderPriceGas = sendOrderPriceTransaction.getGas();
        logger.info("sendOrderPriceTransaction gas: {}", sendOrderPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // send price for shipment from seller
        BigInteger shipmentPriceType = ProductSaleTest.SHIPMENT_PRICE_TYPE;
        TransactionReceipt sendShipmentPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.shipmentPrice, shipmentPriceType);

        assertNotNull(sendShipmentPriceTransactionReceipt);
        assertTrue(sendShipmentPriceTransactionReceipt.isStatusOK());
        logger.info("sendShipmentPriceTransactionReceipt block number {}", sendShipmentPriceTransactionReceipt.getBlockNumber());
        logger.info("sendShipmentPriceTransactionReceipt.getGasUsed: {}", sendShipmentPriceTransactionReceipt.getGasUsed());
        logger.info("sendShipmentPriceTransactionReceipt: {}", sendShipmentPriceTransactionReceipt.toString());

        // get transaction from sendShipmentPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendShipmentPriceTransaction = this.getTransactionForReceipt(sendShipmentPriceTransactionReceipt);

        BigInteger sendShipmentPriceGasPrice = sendShipmentPriceTransaction.getGasPrice();
        logger.info("sendShipmentPriceTransaction gas price: {}", sendShipmentPriceGasPrice);
        BigInteger sendShipmentPriceGas = sendShipmentPriceTransaction.getGas();
        logger.info("sendShipmentPriceTransaction gas: {}", sendShipmentPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendShipmentPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get order number from buyer via PriceSent events
        gasProvider = this.productSale.getGasProvider();
        credentials = this.buyerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        Flowable<ProductSale.PriceSentEventResponse> priceSentFlow = productSale.priceSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.PriceSentEventResponse> priceSentEventResponseList = new ArrayList<>();
        priceSentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(priceSentEventResponse -> {
                    //logger.info("priceSentEventResponse: {}", priceSentEventResponse.toString());
                    priceSentEventResponseList.add(priceSentEventResponse);
                });

        assertNotEquals(0, priceSentEventResponseList.size());

        logger.info("buyer address: {}", this.buyerCredentials.getAddress());
        BigInteger buyerOrderNumber = null;
        for (ProductSale.PriceSentEventResponse priceSentEventResponse : priceSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, priceSentEventResponse.orderno,
                    priceSentEventResponse.buyer, priceSentEventResponse.ttype,
                    priceSentEventResponse.price);
            if (priceSentEventResponse.buyer.equalsIgnoreCase(this.buyerCredentials.getAddress())) {
                buyerOrderNumber = priceSentEventResponse.orderno;
            }
        }

        assertNotNull(buyerOrderNumber);
        logger.info("{} buyerOrderNumber: {}", methodName, buyerOrderNumber.toString());

        // query order from buyer
        Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> order = this.queryOrder(productSale, buyerOrderNumber);

        assertNotNull(order);
        logger.info("order: {}", order.toString());

        String orderBuyerAddress = order.component1();
        String orderProductName = order.component2();
        BigInteger orderQuantity = order.component3();
        BigInteger orderPrice = order.component4();
        assertEquals(this.orderPrice, orderPrice);
        BigInteger shipmentPrice = order.component6();
        assertEquals(this.shipmentPrice, shipmentPrice);

        // send safe payment from buyer
        BigInteger safePayment = orderPrice.add(shipmentPrice);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            //BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();

            org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                    ProductSale.FUNC_SENDSAFEPAY,
                    Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(buyerOrderNumber)),
                    Collections.<TypeReference<?>>emptyList());
            String functionData = FunctionEncoder.encode(function);

            // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
            //estimatedGasProvider.getDefaultGasLimit()
            Transaction safepayTransaction = new Transaction(this.buyerCredentials.getAddress(), BigInteger.ZERO, BigInteger.ZERO,
                    BigInteger.ZERO, this.productSale.getContractAddress(), safePayment, functionData);

            estimatedGasProvider.setTransaction(safepayTransaction);
        }

        TransactionReceipt sendSafepayTransactionReceipt = this.sendSafePayment(productSale, orderNumber, safePayment);

        assertNotNull(sendSafepayTransactionReceipt);
        assertTrue(sendSafepayTransactionReceipt.isStatusOK());
        logger.info("sendSafepayTransactionReceipt block number {}", sendSafepayTransactionReceipt.getBlockNumber());
        logger.info("sendSafepayTransactionReceipt.getGasUsed: {}", sendSafepayTransactionReceipt.getGasUsed());
        logger.info("sendSafepayTransactionReceipt: {}", sendSafepayTransactionReceipt.toString());

        // get transaction from sendSafepay transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendSafepayTransaction = this.getTransactionForReceipt(sendSafepayTransactionReceipt);

        BigInteger sendSafepayGasPrice = sendSafepayTransaction.getGasPrice();
        logger.info("sendSafepayTransaction gas price: {}", sendSafepayGasPrice);
        BigInteger sendSafepayGas = sendSafepayTransaction.getGas();
        logger.info("sendSafepayTransaction gas: {}", sendSafepayGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendSafepayTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        BigInteger contractBalance = this.getBalance(productSale.getContractAddress(), DefaultBlockParameterName.LATEST);

        assertNotNull(contractBalance);

        logger.info("{} safepayment: {}", methodName, safePayment.toString());
        logger.info("{} contract balance: {}", methodName, contractBalance.toString());

        assertEquals(safePayment, contractBalance);

        // send invoice for order from seller
        // TODO: listen for SafepaySent events and get order number
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        TransactionReceipt sendInvoiceTransactionReceipt = this.sendInvoice(productSale, orderNumber, ProductSaleTest.DELIVERY_DATE, this.courierCredentials.getAddress());

        assertNotNull(sendInvoiceTransactionReceipt);
        assertTrue(sendInvoiceTransactionReceipt.isStatusOK());
        logger.info("sendInvoiceTransactionReceipt block number {}", sendInvoiceTransactionReceipt.getBlockNumber());
        logger.info("sendInvoiceTransactionReceipt.getGasUsed: {}", sendInvoiceTransactionReceipt.getGasUsed());
        logger.info("sendInvoiceTransactionReceipt: {}", sendInvoiceTransactionReceipt.toString());

        // get transaction from sendInvoice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendInvoiceTransaction = this.getTransactionForReceipt(sendInvoiceTransactionReceipt);

        BigInteger sendInvoiceGasPrice = sendInvoiceTransaction.getGasPrice();
        logger.info("sendInvoiceTransaction gas price: {}", sendInvoiceGasPrice);
        BigInteger sendInvoiceGas = sendInvoiceTransaction.getGas();
        logger.info("sendInvoiceTransaction gas: {}", sendInvoiceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendInvoiceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get invoice for order from courier
        gasProvider = this.productSale.getGasProvider();
        credentials = this.courierCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        Flowable<ProductSale.InvoiceSentEventResponse> invoiceSentFlow = productSale.invoiceSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.InvoiceSentEventResponse> invoiceSentEventResponseList = new ArrayList<>();
        invoiceSentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(invoiceSentEventResponse -> {
                    //logger.info("invoiceSentEventResponse: {}", invoiceSentEventResponse.toString());
                    invoiceSentEventResponseList.add(invoiceSentEventResponse);
                });

        assertNotEquals(0, invoiceSentEventResponseList.size());

        logger.info("courier address: {}", this.courierCredentials.getAddress());
        BigInteger courierInvoiceNumber = null;
        for (ProductSale.InvoiceSentEventResponse invoiceSentEventResponse : invoiceSentEventResponseList) {
            //String courierName = new String(invoiceSentEventResponse.courier, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, invoiceSentEventResponse.invoiceno,
                    invoiceSentEventResponse.orderno, invoiceSentEventResponse.buyer,
                    invoiceSentEventResponse.courier, invoiceSentEventResponse.deliveryDate);
            if (invoiceSentEventResponse.courier.equalsIgnoreCase(this.courierCredentials.getAddress())) {
                courierInvoiceNumber = invoiceSentEventResponse.invoiceno;
            }
        }

        assertNotNull(courierInvoiceNumber);
        logger.info("{} courierInvoiceNumber: {}", methodName, courierInvoiceNumber.toString());

        // query invoice from courier
        Tuple4<String, BigInteger, BigInteger, String> invoice = this.queryInvoice(productSale, courierInvoiceNumber);
        assertNotNull(invoice);

        logger.info("invoice: {}", invoice.toString());

        String buyerAddress = invoice.component1();
        assertEquals(this.buyerCredentials.getAddress(), buyerAddress);

        BigInteger invoiceOrderNumber = invoice.component2();
        //assertEquals(orderNumber, invoiceOrderNumber);
        assertEquals(buyerOrderNumber, invoiceOrderNumber);

        BigInteger deliveryDate = invoice.component3();
        assertEquals(ProductSaleTest.DELIVERY_DATE, deliveryDate);

        String courierName = invoice.component4();
        assertEquals(this.courierCredentials.getAddress(), courierName);

        // get contract balance before delivery
        BigInteger contractBalanceBeforeDelivery = this.getBalance(productSale.getContractAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(contractBalanceBeforeDelivery);
        logger.info("{} contract balance before delivery: {}", methodName, contractBalanceBeforeDelivery.toString());

        // get seller balance before delivery
        BigInteger sellerBalanceBeforeDelivery = this.getBalance(this.sellerCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(sellerBalanceBeforeDelivery);
        logger.info("{} seller balance before delivery: {}", methodName, sellerBalanceBeforeDelivery.toString());

        // get courier balance before delivery
        BigInteger courierBalanceBeforeDelivery = this.getBalance(this.courierCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(courierBalanceBeforeDelivery);
        logger.info("{} courier balance before delivery: {}", methodName, courierBalanceBeforeDelivery.toString());

        // send delivery from courier
        TransactionReceipt deliveryTransactionReceipt = this.delivery(productSale, courierInvoiceNumber, ProductSaleTest.DELIVERY_DATE);
        assertNotNull(deliveryTransactionReceipt);
        assertTrue(deliveryTransactionReceipt.isStatusOK());
        logger.info("deliveryTransactionReceipt block number {}", deliveryTransactionReceipt.getBlockNumber());
        logger.info("deliveryTransactionReceipt.getGasUsed: {}", deliveryTransactionReceipt.getGasUsed());
        logger.info("deliveryTransactionReceipt: {}", deliveryTransactionReceipt.toString());

        // get transaction from delivery transaction receipt
        org.web3j.protocol.core.methods.response.Transaction deliveryTransaction = this.getTransactionForReceipt(deliveryTransactionReceipt);

        BigInteger deliveryGasPrice = deliveryTransaction.getGasPrice();
        logger.info("deliveryTransaction gas price: {}", deliveryGasPrice);
        BigInteger deliveryGas = deliveryTransaction.getGas();
        logger.info("deliveryTransaction gas: {}", deliveryGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("deliveryTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get contract balance after delivery
        BigInteger contractBalanceAfterDelivery = this.getBalance(productSale.getContractAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(contractBalanceAfterDelivery);
        logger.info("{} contract balance after delivery: {}", methodName, contractBalanceAfterDelivery.toString());

        BigInteger contractBalanceDifference = contractBalanceBeforeDelivery.subtract(contractBalanceAfterDelivery);
        logger.info("{} safe payment value: {}", methodName, safePayment.toString());
        logger.info("{} contract balance difference: {}", methodName, contractBalanceDifference);

        assertEquals(safePayment, contractBalanceDifference);

        // get seller balance after delivery
        BigInteger sellerBalanceAfterDelivery = this.getBalance(this.sellerCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(sellerBalanceAfterDelivery);
        logger.info("{} seller balance after delivery: {}", methodName, sellerBalanceAfterDelivery.toString());

        BigInteger sellerBalanceDifference = sellerBalanceAfterDelivery.subtract(sellerBalanceBeforeDelivery);
        logger.info("{} order price: {}", methodName, orderPrice.toString());
        logger.info("{} seller balance difference: {}", methodName, sellerBalanceDifference.toString());

        assertEquals(orderPrice, sellerBalanceDifference);

        // get courier balance after delivery
        BigInteger courierBalanceAfterDelivery = this.getBalance(this.courierCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(courierBalanceAfterDelivery);
        logger.info("{} courier balance after delivery: {}", methodName, courierBalanceAfterDelivery.toString());
        logger.info("{} courier balance before deliver: {}", methodName, courierBalanceBeforeDelivery.toString());

        BigInteger deliveryTransactionFee = deliveryGas.multiply(deliveryGasPrice);
        logger.info("{} delivery transaction fee: {}", methodName, deliveryTransactionFee.toString());

        BigInteger courierBalanceDifference = BigInteger.ZERO;
        if (courierBalanceAfterDelivery.compareTo(courierBalanceBeforeDelivery) >= 0) {
            courierBalanceDifference = courierBalanceBeforeDelivery.add(deliveryTransactionFee);
            courierBalanceDifference = courierBalanceDifference.subtract(courierBalanceAfterDelivery);
        } else {
            // courierBalanceAfterDelivery < courierBalanceBeforeDelivery
            logger.info("courierBalanceAfterDelivery < courierBalanceBeforeDelivery");
            //courierBalanceDifference = courierBalanceAfterDelivery.add(deliveryTransactionFee);
            //logger.info("{} contractBalanceAfterDelivery + delivery transaction fee: {}", methodName, courierBalanceDifference);
            //courierBalanceDifference = courierBalanceDifference.subtract(courierBalanceBeforeDelivery);

            /*
            BigInteger test = courierBalanceBeforeDelivery.subtract(courierBalanceAfterDelivery);
            logger.info("{} courierBalanceBeforeDelivery - courierBalanceAfterDelivery: {}", methodName, test);
            test = deliveryTransactionFee.subtract(test);
            logger.info("{} txfee - courierBalanceBeforeDelivery - courierBalanceAfterDelivery: {}", methodName, test);
            */

            deliveryTransactionFee = deliveryTransactionFee.subtract(shipmentPrice);
            courierBalanceDifference = courierBalanceBeforeDelivery.subtract(courierBalanceAfterDelivery);
        }

        logger.info("{} shipment price: {}", methodName, shipmentPrice.toString());
        logger.info("{} courier balance difference: {}", methodName, courierBalanceDifference.toString());

        //assertEquals(shipmentPrice, courierBalanceDifference);

        logger.info("{} corrected delivery transaction fee: {}", methodName, deliveryTransactionFee);

        assertEquals(true, (courierBalanceDifference.compareTo(deliveryTransactionFee) <= 0));

        // get product id via production validation contract
        // and OwnershipTransferred events

        Flowable<Product.OwnershipTransferredEventResponse> ownershipTransferredEventResponseFlowable = productValidationContract.ownershipTransferredEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<Product.OwnershipTransferredEventResponse> ownershipTransferredEventResponseList = new ArrayList<>();
        ownershipTransferredEventResponseFlowable.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(ownershipTransferredEventResponse -> {
                    ownershipTransferredEventResponseList.add(ownershipTransferredEventResponse);
                });

        assertNotEquals(0, ownershipTransferredEventResponseList.size());

        String buyerAddressName = this.buyerCredentials.getAddress().toLowerCase();
        if (buyerAddressName.startsWith("0x")) {
            buyerAddressName = buyerAddressName.substring(2);
        }

        BigInteger productId = null;
        for (Product.OwnershipTransferredEventResponse ownershipTransferredEventResponse : ownershipTransferredEventResponseList) {
            logger.info("{} ownershipTransferredEvent: {} {}", methodName, ownershipTransferredEventResponse.productOwnerName,
                    ownershipTransferredEventResponse.productId);
            String productOwner = ownershipTransferredEventResponse.productOwnerName;

            if (productOwner.equalsIgnoreCase(buyerAddressName)) {
                productId = ownershipTransferredEventResponse.productId;
            }
        }

        assertNotNull(productId);
        logger.info("{} product id: {}", methodName, productId.toString());

        // get product information via production validation contract

        RemoteFunctionCall<Tuple3<String, String, BigInteger>> getProductFromIdRemoteFuncCall = productValidationContract.getProductFromProductId(productId);
        Tuple3<String, String, BigInteger> productInfo = null;
        try {
            productInfo = getProductFromIdRemoteFuncCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
        }

        assertNotNull(productInfo);
        logger.info("{} product info: {}", methodName, productInfo.toString());

        String productOwner = productInfo.component1();
        assertEquals(buyerAddressName, productOwner);

        String productName = productInfo.component2();
        assertEquals(this.productName, productName);
    }

    protected TransactionReceipt sendRefund(ProductSale productSale, BigInteger orderNumber) {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        RemoteFunctionCall<TransactionReceipt> sendRefundRemoteFuncCall = productSale.sendRefund(orderNumber);
        TransactionReceipt transactionReceipt = null;
        try {
            transactionReceipt = sendRefundRemoteFuncCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            return null;
        }

        return transactionReceipt;
    }

    @Test
    @Disabled
    public void testSendRefund() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        //EstimatedGasProvider estimGasProvider = (EstimatedGasProvider) gasProvider;
        //estimGasProvider.setDefaultGasLimit(null);
        //gasProvider = estimGasProvider;
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.buyerCredentials;
        //Credentials credentials = this.courierCredentials;
        ProductSale productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // send order from buyer
        TransactionReceipt transactionReceipt = this.sendOrder(productSale, this.productName, this.productQuantity);

        assertNotNull(transactionReceipt);
        assertTrue(transactionReceipt.isStatusOK());
        logger.info("sendOrderTransactionReceipt block number {}", transactionReceipt.getBlockNumber());
        logger.info("sendOrderTransactionReceipt.getGasUsed: {}", transactionReceipt.getGasUsed());
        logger.info("sendOrderTransactionReceipt: {}", transactionReceipt.toString());

        // get transaction from transaction receipt
        org.web3j.protocol.core.methods.response.Transaction transaction = this.getTransactionForReceipt(transactionReceipt);

        BigInteger gasPrice = transaction.getGasPrice();
        logger.info("sendOrderTransaction gas price: {}", gasPrice);
        BigInteger gas = transaction.getGas();
        logger.info("sendOrderTransaction gas: {}", gas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get latest orders from seller
        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // create sendOrderFlow
        Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.OrderSentEventResponse> orderSentEventResponseList = new ArrayList<>();
        sendOrderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(orderSentEventResponse -> {
                    orderSentEventResponseList.add(orderSentEventResponse);
                });

        BigInteger orderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : orderSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            orderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(orderNumber);
        logger.info("{} orderNumber: {}", methodName, orderNumber.toString());

        // send price for order from seller
        BigInteger priceType = ProductSaleTest.ORDER_PRICE_TYPE;
        TransactionReceipt sendOrderPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.orderPrice, priceType);

        assertNotNull(sendOrderPriceTransactionReceipt);
        assertTrue(sendOrderPriceTransactionReceipt.isStatusOK());
        logger.info("sendOrderPriceTransactionReceipt block number {}", sendOrderPriceTransactionReceipt.getBlockNumber());
        logger.info("sendOrderPriceTransactionReceipt.getGasUsed: {}", sendOrderPriceTransactionReceipt.getGasUsed());
        logger.info("sendOrderPriceTransactionReceipt: {}", sendOrderPriceTransactionReceipt.toString());

        // get transaction from sendOrderPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendOrderPriceTransaction = this.getTransactionForReceipt(sendOrderPriceTransactionReceipt);

        BigInteger sendOrderPriceGasPrice = sendOrderPriceTransaction.getGasPrice();
        logger.info("sendOrderPriceTransaction gas price: {}", sendOrderPriceGasPrice);
        BigInteger sendOrderPriceGas = sendOrderPriceTransaction.getGas();
        logger.info("sendOrderPriceTransaction gas: {}", sendOrderPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendOrderPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // send price for shipment from seller
        BigInteger shipmentPriceType = ProductSaleTest.SHIPMENT_PRICE_TYPE;
        TransactionReceipt sendShipmentPriceTransactionReceipt = this.sendPrice(productSale, orderNumber, this.shipmentPrice, shipmentPriceType);

        assertNotNull(sendShipmentPriceTransactionReceipt);
        assertTrue(sendShipmentPriceTransactionReceipt.isStatusOK());
        logger.info("sendShipmentPriceTransactionReceipt block number {}", sendShipmentPriceTransactionReceipt.getBlockNumber());
        logger.info("sendShipmentPriceTransactionReceipt.getGasUsed: {}", sendShipmentPriceTransactionReceipt.getGasUsed());
        logger.info("sendShipmentPriceTransactionReceipt: {}", sendShipmentPriceTransactionReceipt.toString());

        // get transaction from sendShipmentPrice transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendShipmentPriceTransaction = this.getTransactionForReceipt(sendShipmentPriceTransactionReceipt);

        BigInteger sendShipmentPriceGasPrice = sendShipmentPriceTransaction.getGasPrice();
        logger.info("sendShipmentPriceTransaction gas price: {}", sendShipmentPriceGasPrice);
        BigInteger sendShipmentPriceGas = sendShipmentPriceTransaction.getGas();
        logger.info("sendShipmentPriceTransaction gas: {}", sendShipmentPriceGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendShipmentPriceTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get order number from buyer
        gasProvider = this.productSale.getGasProvider();
        credentials = this.buyerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        Flowable<ProductSale.OrderSentEventResponse> orderFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.OrderSentEventResponse> priceSentEventResponseList = new ArrayList<>();
        orderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(priceSentEventResponse -> {
                    //logger.info("priceSentEventResponse: {}", priceSentEventResponse.toString());
                    logger.info("orderSentEventResponse: {} {}", priceSentEventResponse.orderno, priceSentEventResponse.buyer);
                    priceSentEventResponseList.add(priceSentEventResponse);
                });

        assertNotEquals(0, priceSentEventResponseList.size());

        BigInteger buyerOrderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : priceSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            buyerOrderNumber = orderSentEventResponse.orderno;
        }

        assertNotNull(buyerOrderNumber);
        logger.info("{} buyerOrderNumber: {}", methodName, buyerOrderNumber.toString());

        // query order from buyer
        Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> order = this.queryOrder(productSale, buyerOrderNumber);

        assertNotNull(order);
        logger.info("order: {}", order.toString());

        String orderBuyerAddress = order.component1();
        String orderProductName = order.component2();
        BigInteger orderQuantity = order.component3();
        BigInteger orderPrice = order.component4();
        assertEquals(this.orderPrice, orderPrice);
        BigInteger shipmentPrice = order.component6();
        assertEquals(this.shipmentPrice, shipmentPrice);

        // send safe payment from buyer
        BigInteger safePayment = orderPrice.add(shipmentPrice);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();

            org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                    ProductSale.FUNC_SENDSAFEPAY,
                    Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(buyerOrderNumber)),
                    Collections.<TypeReference<?>>emptyList());
            String functionData = FunctionEncoder.encode(function);

            // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
            Transaction safepayTransaction = new Transaction(this.buyerCredentials.getAddress(), BigInteger.ZERO, BigInteger.ZERO,
                    estimatedGasProvider.getDefaultGasLimit(), this.productSale.getContractAddress(), safePayment, functionData);

            estimatedGasProvider.setTransaction(safepayTransaction);
            //estimatedGasProvider.setGasPriceMultiplier(1.1);
        }

        TransactionReceipt sendSafepayTransactionReceipt = this.sendSafePayment(productSale, orderNumber, safePayment);

        assertNotNull(sendSafepayTransactionReceipt);
        assertTrue(sendSafepayTransactionReceipt.isStatusOK());
        logger.info("sendSafepayTransactionReceipt block number {}", sendSafepayTransactionReceipt.getBlockNumber());
        logger.info("sendSafepayTransactionReceipt.getGasUsed: {}", sendSafepayTransactionReceipt.getGasUsed());
        logger.info("sendSafepayTransactionReceipt: {}", sendSafepayTransactionReceipt.toString());

        // get transaction from sendSafepay transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendSafepayTransaction = this.getTransactionForReceipt(sendSafepayTransactionReceipt);

        BigInteger sendSafepayGasPrice = sendSafepayTransaction.getGasPrice();
        logger.info("sendSafepayTransaction gas price: {}", sendSafepayGasPrice);
        BigInteger sendSafepayGas = sendSafepayTransaction.getGas();
        logger.info("sendSafepayTransaction gas: {}", sendSafepayGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendSafepayTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get safe payments for orders from seller

        gasProvider = this.productSale.getGasProvider();
        credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        Flowable<ProductSale.SafepaySentEventResponse> safepaySentFlow = productSale.safepaySentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.SafepaySentEventResponse> safepaySentEventResponseList = new ArrayList<>();
        safepaySentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(safepaySentEventResponse -> {
                    logger.info("safepaySentEventResponse: {}", safepaySentEventResponse.toString());
                    safepaySentEventResponseList.add(safepaySentEventResponse);
                });

        assertNotEquals(0, safepaySentEventResponseList.size());

        BigInteger sellerOrderNumber = null;
        for (ProductSale.SafepaySentEventResponse safepaySentEventResponse : safepaySentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, safepaySentEventResponse.orderno,
                    safepaySentEventResponse.buyer, safepaySentEventResponse.value,
                    safepaySentEventResponse.now);
            sellerOrderNumber = safepaySentEventResponse.orderno;
        }

        assertNotNull(sellerOrderNumber);
        logger.info("{} sellerOrderNumber: {}", methodName, sellerOrderNumber.toString());

        // query order from seller
        Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> orderForSeller = this.queryOrder(productSale, sellerOrderNumber);

        assertNotNull(orderForSeller);
        logger.info("orderForSeller: {}", orderForSeller.toString());

        orderBuyerAddress = orderForSeller.component1();
        orderProductName = orderForSeller.component2();
        orderQuantity = orderForSeller.component3();
        orderPrice = orderForSeller.component4();
        BigInteger safepay = orderForSeller.component5();
        shipmentPrice = orderForSeller.component6();

        BigInteger expectedSafepay = orderPrice.add(shipmentPrice);
        assertEquals(expectedSafepay, safepay);

        // get and test contract balance

        BigInteger contractBalance = this.getBalance(productSale.getContractAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(contractBalance);

        logger.info("{} safepayment: {}", methodName, safePayment.toString());
        logger.info("{} contract balance: {}", methodName, contractBalance.toString());

        assertEquals(safePayment, contractBalance);

        // get buyer balance before refund
        BigInteger buyerBalanceBeforeRefund = this.getBalance(this.buyerCredentials.getAddress(), DefaultBlockParameterName.LATEST);

        // send refund
        TransactionReceipt sendRefundTransactionReceipt = this.sendRefund(productSale, sellerOrderNumber);

        assertNotNull(sendRefundTransactionReceipt);
        assertTrue(sendRefundTransactionReceipt.isStatusOK());
        logger.info("sendRefundTransactionReceipt block number {}", sendRefundTransactionReceipt.getBlockNumber());
        logger.info("sendRefundTransactionReceipt.getGasUsed: {}", sendRefundTransactionReceipt.getGasUsed());
        logger.info("sendRefundTransactionReceipt: {}", sendRefundTransactionReceipt.toString());

        // get transaction from sendRefund transaction receipt
        org.web3j.protocol.core.methods.response.Transaction sendRefundTransaction = this.getTransactionForReceipt(sendRefundTransactionReceipt);

        BigInteger sendRefundGasPrice = sendRefundTransaction.getGasPrice();
        logger.info("sendRefundTransaction gas price: {}", sendRefundGasPrice);
        BigInteger sendRefundGas = sendRefundTransaction.getGas();
        logger.info("sendRefundTransaction gas: {}", sendRefundGas);

        if (productSale.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)productSale.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("sendRefundTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get buyer balance after refund
        BigInteger buyerBalanceAfterRefund = this.getBalance(this.buyerCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        logger.info("{} buyer balance before refund: {}", methodName, buyerBalanceBeforeRefund.toString());
        logger.info("{} buyer balance after refund:  {}", methodName, buyerBalanceAfterRefund.toString());

        assertTrue((buyerBalanceAfterRefund.compareTo(buyerBalanceBeforeRefund) > 0));

        // get order and test safepay value

        orderForSeller = this.queryOrder(productSale, sellerOrderNumber);

        assertNotNull(orderForSeller);
        logger.info("orderForSeller: {}", orderForSeller.toString());

        //orderBuyerAddress = orderForSeller.component1();
        //orderProductName = orderForSeller.component2();
        //orderQuantity = orderForSeller.component3();
        //orderPrice = orderForSeller.component4();
        BigInteger refundedSafepay = orderForSeller.component5();
        //shipmentPrice = orderForSeller.component6();

        assertEquals(BigInteger.ZERO, refundedSafepay);

        // get and test contract balance
        contractBalance = this.getBalance(productSale.getContractAddress(), DefaultBlockParameterName.LATEST);
        assertNotNull(contractBalance);

        logger.info("{} contract balance: {}", methodName, contractBalance.toString());

        assertEquals(BigInteger.ZERO, contractBalance);

        // test RefundSent event
        Flowable<ProductSale.RefundSentEventResponse> refundSentFlow = productSale.refundSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<ProductSale.RefundSentEventResponse> refundSentEventResponseList = new ArrayList<>();
        refundSentFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(refundSentEventResponse -> {
                    refundSentEventResponseList.add(refundSentEventResponse);
                });

        assertNotEquals(0, refundSentEventResponseList.size());

        String refundAddress = null;
        BigInteger refundValue = null;
        for (ProductSale.RefundSentEventResponse refundSentEventResponse : refundSentEventResponseList) {
            logger.info("{} {} {} {} {}", methodName, refundSentEventResponse._from,
                    refundSentEventResponse._to, refundSentEventResponse._value,
                    refundSentEventResponse._timestamp);
            refundAddress = refundSentEventResponse._to;
            refundValue = refundSentEventResponse._value;
        }

        assertNotNull(refundAddress);
        assertNotNull(refundValue);

        assertEquals(this.buyerCredentials.getAddress(), refundAddress);
        assertEquals(refundedSafepay, refundValue);
    }

    @Test
    //@Disabled
    public void testOrderSentEvents() {
        String methodName = ReflectionHelper.getMethodName(ProductSaleTest.class);
        logger.info("==== {} ====", methodName);

        // get latest orders from seller
        ContractGasProvider gasProvider = this.productSale.getGasProvider();
        Credentials credentials = this.sellerCredentials;
        productSale = ProductSale.load(this.productSale.getContractAddress(), this.web3j, credentials, gasProvider);

        // create sendOrderFlow
        Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        // create sendOrderFlow with custom EthFilter
        //org.web3j.protocol.core.methods.request.EthFilter orderSentEventFilter = new org.web3j.protocol.core.methods.request.EthFilter(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST, this.productSale.getContractAddress());
        //String topic = EventEncoder.encode(ProductSale.ORDERSENT_EVENT);
        //orderSentEventFilter.addSingleTopic(topic);
        //orderSentEventFilter.addOptionalTopics(String eventParam1, String eventParam2);
        //Flowable<ProductSale.OrderSentEventResponse> sendOrderFlow = productSale.orderSentEventFlowable(orderSentEventFilter);

        //sendOrderFlow.map(orderSentEventResponse -> orderSentEventResponse.orderno)
        //.forEach(bigInteger -> System.out.println(bigInteger));
        /*
        sendOrderFlow.subscribe(orderSentEventResponse -> {
            logger.info("{} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.goods,
                    orderSentEventResponse.quantity);
        });
        */

        List<ProductSale.OrderSentEventResponse> orderSentEventResponseList = new ArrayList<>();
        sendOrderFlow.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(orderSentEventResponse -> {
                    orderSentEventResponseList.add(orderSentEventResponse);
                });

        //orderSentEventResponseList = sendOrderFlow.timeout(1, TimeUnit.SECONDS).toList().blockingGet();
        //orderSentEventResponseList = productSale.getOrderSentEvents(transactionReceipt);

        BigInteger orderNumber = null;
        for (ProductSale.OrderSentEventResponse orderSentEventResponse : orderSentEventResponseList) {
            //String productName = new String(orderSentEventResponse.productName, StandardCharsets.UTF_8);
            logger.info("{} {} {} {} {}", methodName, orderSentEventResponse.orderno,
                    orderSentEventResponse.buyer, orderSentEventResponse.productName,
                    orderSentEventResponse.quantity);
            orderNumber = orderSentEventResponse.orderno;
        }

        //assertNotNull(orderNumber);
        //logger.info("{} orderNumber: {}", methodName, orderNumber.toString());
        assertNotNull(orderSentEventResponseList);
    }

    @AfterAll
    public void tearDownTests() {
        logger.info("logBalanceCallCount: {}", this.logBalanceCallCount);
        logger.info("==========");
    }
}