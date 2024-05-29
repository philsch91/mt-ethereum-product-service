package at.schunker.mt.ethereumproductservice.contract;

import at.schunker.mt.ethereumproductservice.logging.HttpLogger;
import at.schunker.mt.ethereumproductservice.tx.EstimatedGasProvider;
import at.schunker.mt.ethereumproductservice.util.CredentialUtils;
import at.schunker.mt.ethereumproductservice.util.ReflectionHelper;
import io.reactivex.Flowable;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
//import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tx.gas.ContractGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

//import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
class ProductTest extends BaseContractTest {

    Logger logger = LogManager.getLogger(ProductTest.class);

    private static final String PRODUCT_NAME = "Cheese";

    private Product product = null;
    @Value("${productcontract.address:notset}")
    protected String contractAddress;

    @Value("${seller.privatekey}")
    private String sellerPrivateKey;

    // BIP-39 (+ BIP-32 & BIP-44)
    @Value("${seller.mnemonic}")
    private String sellerMnemonic;
    @Value("${buyer.password}")
    private String buyerPassword;
    @Value("${buyer.mnemonic:default}")
    private String buyerMnemonic;
    @Value("${courier.password}")
    private String courierPassword;
    @Value("${courier.mnemonic:default}")
    private String courierMnemonic;

    // Credentials
    private Credentials sellerCredentials = null;
    private Credentials buyerCredentials = null;
    private Credentials courierCredentials = null;

    private String productOwner = null;

    private ProductSale productSaleContract = null;
    @Value("${productsalecontract.address:notset}")
    private String productSaleContractAddress;

    @BeforeAll
    public void setupTests() throws URISyntaxException {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
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
        //this.sellerCredentials = Credentials.create(this.sellerPrivateKey);
        //this.buyerCredentials = Credentials.create("dc54784b77b6af4472d6af058a3739f853ee9f000f9f1b655934be50706d4931");
        //this.courierCredentials = Credentials.create("2dc023d6a5267d130ea5329b8a18180c8e26cfe4a5182fb34623d5db25fcfbb7");

        // created with MetaMask
        this.sellerCredentials = Credentials.create(this.sellerPrivateKey);
        ////this.sellerCredentials = WalletUtils.loadBip39Credentials("password", "mnemonic");
        // created with Web3j
        this.buyerCredentials = Bip44WalletUtils.loadBip44Credentials(this.buyerPassword, this.sellerMnemonic);
        this.courierCredentials = Bip44WalletUtils.loadBip44Credentials(this.courierPassword, this.sellerMnemonic);
        //
        /*
        //Bip39Wallet bip39Wallet = new Bip39Wallet("filename", "mnemonic");
        Bip39Wallet bip39Wallet = null;
        try {
            bip39Wallet = WalletUtils.generateBip39Wallet("password", new java.io.File("pathname"));
        } catch (CipherException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        bip39Wallet.getMnemonic();
        */

        String sellerPrivateKey = CredentialUtils.getPrivateKeyInCredentials(this.sellerCredentials);
        String buyerPrivateKey = CredentialUtils.getPrivateKeyInCredentials(this.buyerCredentials);
        String courierPrivateKey = CredentialUtils.getPrivateKeyInCredentials(this.courierCredentials);

        logger.info("seller private key: {}", sellerPrivateKey);
        logger.info("buyer private key: {}", buyerPrivateKey);
        logger.info("courier private key: {}", courierPrivateKey);

        logger.info("seller address: {}", this.sellerCredentials.getAddress());
        logger.info("buyer address: {}", this.buyerCredentials.getAddress());
        logger.info("courier address: {}", this.courierCredentials.getAddress());

        this.buyerCredentials.getAddress().toLowerCase().substring(2);

        BigInteger sellerBalance = this.getBalance(this.sellerCredentials.getAddress(), DefaultBlockParameterName.LATEST);
        logger.info("{} seller balance: {}", methodName, sellerBalance.toString());

        BigInteger transactionCount = this.getTransactionCount(this.sellerCredentials.getAddress(), DefaultBlockParameterName.PENDING);
        logger.info("{} transaction count: {}", methodName, transactionCount.toString());

        EthBlock.Block block = this.getBlockByNumber(DefaultBlockParameterName.LATEST);
        logger.info("{} latest block logs bloom: {}", methodName, block.getLogsBloom());
        /*
        String baseFee = block.getBaseFeePerGas();
        if (baseFee.startsWith("0x")) {
            baseFee = baseFee.substring(2);
        }
        BigInteger baseFeePerGas = new BigInteger(baseFee, 16);
        */
        BigInteger baseFeePerGas = block.getBaseFeePerGas();
        logger.info("{} latest block base fee per gas: {}", methodName, baseFeePerGas.toString());
        // roots
        logger.info("{} latest block transactions roots: {}", methodName, block.getTransactionsRoot());
        logger.info("{} latest block state roots: {}", methodName, block.getStateRoot());
        logger.info("{} latest block receipts roots: {}", methodName, block.getReceiptsRoot());
        // PoW
        logger.info("{} latest block difficulty: {}", methodName, block.getDifficulty().toString());
        logger.info("{} latest block mix hash: {}", methodName, block.getMixHash());
        logger.info("{} latest block nonce: {}", methodName, block.getNonce().toString());

        // ContractGasProvider setup
        EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        gasProvider.setDefaultGasLimit(null);   // enables call of ethGetBlockByNumber in getLatestGasLimit of EstimatedGasProvider
        //gasProvider.setGasPriceMultiplier(1.3);
        //gasProvider.setGasPriceMultiplier(1.0);
        gasProvider.setGasPriceMultiplier(0.5);

        // Contract deployment
        /*
        // Product.BINARY
        // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
        String from = this.sellerCredentials.getAddress();
        Transaction transaction = new Transaction(from, BigInteger.ZERO, BigInteger.ZERO,
                BigInteger.ZERO, null, BigInteger.ZERO, Product.BINARY);

        gasProvider.setTransaction(transaction);

        RemoteCall<Product> remoteCall = Product.deploy(this.web3j, this.sellerCredentials, gasProvider);
        Product productContract = null;
        try {
            productContract = remoteCall.send();
        } catch (Exception ex) {
            //System.err.println(ex.getMessage());
            logger.error(ex.getMessage());
        }

        assertNotNull(productContract);
        this.product = productContract;
        String contractAddress = productContract.getContractAddress();
        assertNotNull(contractAddress);

        logger.info("product contract address: {}", this.product.getContractAddress());
        */

        // Contract load
        //this.product = Product.load(this.contractAddress, this.web3j, this.sellerCredentials, gasProvider);

        //List<Credentials> credentialList = Arrays.asList(this.sellerCredentials, this.buyerCredentials, this.courierCredentials);
        this.credentialMap = Map.of(
                "seller", this.sellerCredentials,
                "buyer", this.buyerCredentials,
                "courier", this.courierCredentials);
    }

    @Test
    //@Disabled
    public void testCustomContractDeployment() {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
        logger.info("==== {} ====", methodName);

        String data = Product.BINARY;
        //BigInteger gasLimit = BigInteger.valueOf(1283823L); //1283823L
        BigInteger nonce = BigInteger.valueOf(3L);
        Credentials credentials = this.buyerCredentials; // this.sellerCredentials

        // EstimatedGasProvider for gas limit

        EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        gasProvider.setDefaultGasLimit(null);
        gasProvider.setGasPriceMultiplier(0.5); // 1.0

        // new Transaction(String from, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, BigInteger value, String data)
        //String from = null
        String from = credentials.getAddress();
        Transaction transaction = new Transaction(from, BigInteger.ZERO, BigInteger.ZERO, gasProvider.getDefaultGasLimit(), null, BigInteger.ZERO, data);

        gasProvider.setTransaction(transaction);
        BigInteger gasLimit = gasProvider.getGasLimit();
        logger.info("{} gas limit: {}", methodName, gasLimit.toString());

        // Deployment

        //String transactionHash = this.deployWithCustomNonce(data, gasLimit, nonce, credentials);
        String transactionHash = this.deployWithPolling(data, gasLimit, credentials);
        assertNotNull(transactionHash);

        org.web3j.protocol.core.methods.response.Transaction contractCreationTransaction = this.getTransactionByHash(transactionHash);
        assertNotNull(contractCreationTransaction);
        logger.info("{} {}", methodName, contractCreationTransaction.toString());
        assertEquals(nonce, contractCreationTransaction.getNonce());
    }

    @Test
    @Disabled
    public void testCancelTransaction() {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
        logger.info("==== {} ====", methodName);

        BigInteger nonce = BigInteger.ZERO;
        BigInteger gasLimit = BigInteger.valueOf(1283825L);
        String transactionHash = this.cancelTransaction(this.sellerCredentials, nonce, gasLimit);
        org.web3j.protocol.core.methods.response.Transaction transaction = this.getTransactionByHash(transactionHash);
        assertNotNull(transaction);
        logger.info("{} {}", methodName, transaction);
        assertEquals(BigInteger.ZERO, transaction.getValue());
    }

    @Test
    @Disabled("testContractDeploy disabled")
    public void testContractDeploy() {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
        logger.info("==== {} ====", methodName);

        // eth_getTransactionReceipt
        Optional<TransactionReceipt> optTransactionReceipt = this.product.getTransactionReceipt();
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
    @Disabled
    public void testCredentials() {
        String sellerPrivateKey = CredentialUtils.getPrivateKeyInCredentials(this.sellerCredentials);
        assertEquals(this.sellerPrivateKey, sellerPrivateKey);
    }

    @Test
    @Disabled
    public void testSaleContractAddress() {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
        logger.info("==== {} ====", methodName);

        // Sale contract deployment
        /*
        // ContractGasProvider setup
        EstimatedGasProvider estimatedGasProvider = new EstimatedGasProvider(this.web3j);
        //estimatedGasProvider.setDefaultGasLimit(null);
        //estimatedGasProvider.setGasPriceMultiplier(1.1);

        if (this.product.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            estimatedGasProvider = (EstimatedGasProvider)this.product.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
        }

        // Product.BINARY
        // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
        String from = this.sellerCredentials.getAddress();
        Transaction transaction = new Transaction(from, BigInteger.ZERO, BigInteger.ZERO,
                estimatedGasProvider.getDefaultGasLimit(), null, BigInteger.ZERO, ProductSale.BINARY);
        estimatedGasProvider.setTransaction(transaction);

        RemoteCall<ProductSale> remoteCall = ProductSale.deploy(this.web3j, this.sellerCredentials, estimatedGasProvider);
        ProductSale productSaleContract = null;
        try {
            productSaleContract = remoteCall.send();
        } catch (Exception ex) {
            //System.err.println(ex.getMessage());
            //ex.printStackTrace();
            logger.error(ex.getMessage());
        }

        assertNotNull(productSaleContract);
        this.productSaleContract = productSaleContract;

        String newSaleContractAddress = productSaleContract.getContractAddress();
        assertNotNull(newSaleContractAddress);
        this.productSaleContractAddress = newSaleContractAddress;

        logger.info("product sale contract address: {}", this.productSaleContract.getContractAddress());

        Optional<TransactionReceipt> optTransactionReceipt = this.productSaleContract.getTransactionReceipt();
        assertTrue(optTransactionReceipt.isPresent());

        TransactionReceipt validationContractDeploymentTransactionReceipt = optTransactionReceipt.get();
        assertNotNull(validationContractDeploymentTransactionReceipt);
        assertTrue(validationContractDeploymentTransactionReceipt.isStatusOK());

        BigInteger blockNumber = validationContractDeploymentTransactionReceipt.getBlockNumber();
        assertNotNull(blockNumber);

        logger.info("saleContractDeploymentTransactionReceipt block number {}", blockNumber);
        logger.info("saleContractDeploymentTransactionReceipt.getGasUsed: {}", validationContractDeploymentTransactionReceipt.getGasUsed());
        logger.info("saleContractDeploymentTransactionReceipt: {}", validationContractDeploymentTransactionReceipt.toString());
        */

        //EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3j);
        //gasProvider.setDefaultGasLimit(null);
        //gasProvider.setGasPriceMultiplier(1.1);

        ContractGasProvider gasProvider = this.product.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.sellerCredentials;
        //Credentials credentials = this.courierCredentials;
        Product product = Product.load(this.product.getContractAddress(), this.web3j, credentials, gasProvider);

        // set sale contract address
        RemoteFunctionCall<TransactionReceipt> setRemoteFunctionCall = product.setProductSaleContractAddress(this.productSaleContractAddress);
        //RemoteFunctionCall<TransactionReceipt> setRemoteFunctionCall = product.setProductSaleContractAddress(newSaleContractAddress);
        TransactionReceipt setSaleContractTransactionReceipt = null;

        try {
            setSaleContractTransactionReceipt = setRemoteFunctionCall.send();
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

        if (product.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)product.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("setSaleContractTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get sale contract address
        RemoteFunctionCall<String> getSaleContractRemoteFunctionCall = product.getProductSaleContractAddress();
        String productSaleContractAddress = null;

        try {
            productSaleContractAddress = getSaleContractRemoteFunctionCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            assertNotNull(null);
        }

        logger.info("{} product sale contract address: {}", methodName, this.productSaleContractAddress);
        //logger.info("{} new product sale contract address: {}", methodName, newSaleContractAddress);
        logger.info("{} set and returned product sale contract address: {}", methodName, productSaleContractAddress);

        assertEquals(this.productSaleContractAddress, productSaleContractAddress);
        //assertEquals(newSaleContractAddress, productSaleContractAddress);
    }

    protected TransactionReceipt addProduct(Product contract, String productOwner, String productName) {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
        logger.info("==== {} ====", methodName);

        RemoteFunctionCall<TransactionReceipt> addProductRemoteFunctionCall = contract.addProduct(productOwner, productName);
        TransactionReceipt transactionReceipt = null;
        try {
            transactionReceipt = addProductRemoteFunctionCall.send();
        } catch (Exception ex) {
            logger.error("{} {}", methodName, ex.getMessage());
            return null;
        }

        return transactionReceipt;
    }

    protected Tuple3<String, String, BigInteger> getProductFromId(Product product, BigInteger productId) {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
        logger.info("==== {} ====", methodName);

        RemoteFunctionCall<Tuple3<String, String, BigInteger>> getProductFromIdRemoteFuncCall = product.getProductFromProductId(productId);
        Tuple3<String, String, BigInteger> productInfo = null;
        try {
            productInfo = getProductFromIdRemoteFuncCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            return null;
        }

        return productInfo;
    }

    protected Boolean validateProduct(Product product, BigInteger productId, String productName) {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
        logger.info("==== {} ====", methodName);

        RemoteFunctionCall<Boolean> isProductValidRemoteFuncCall = product.isProduct(productId, productName);
        Boolean isProductValid = null;
        try {
            isProductValid = isProductValidRemoteFuncCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            return null;
        }

        return isProductValid;
    }

    protected List<BigInteger> getAllProductIds(Product product) {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
        logger.info("==== {} ====", methodName);

        RemoteFunctionCall<List> getProductIdsRemoteFuncCall = product.getAllProductIds();
        List<BigInteger> productIdList = null;
        try {
            productIdList = getProductIdsRemoteFuncCall.send();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("{} {}", methodName, ex.getMessage());
            return null;
        }

        return productIdList;
    }

    @Test
    @Disabled
    public void testProductValidation() {
        String methodName = ReflectionHelper.getMethodName(ProductTest.class);
        logger.info("==== {} ====", methodName);

        ContractGasProvider gasProvider = this.product.getGasProvider();
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        Credentials credentials = this.sellerCredentials;
        //Credentials credentials = this.courierCredentials;
        Product product = Product.load(this.product.getContractAddress(), this.web3j, credentials, gasProvider);

        TransactionReceipt addProductTransactionReceipt = this.addProduct(product, this.productOwner, ProductTest.PRODUCT_NAME);
        assertNotNull(addProductTransactionReceipt);

        // transaction receipt

        assertTrue(addProductTransactionReceipt.isStatusOK());
        logger.info("addProductTransactionReceipt block number {}", addProductTransactionReceipt.getBlockNumber());
        logger.info("addProductTransactionReceipt.getGasUsed: {}", addProductTransactionReceipt.getGasUsed());
        logger.info("addProductTransactionReceipt: {}", addProductTransactionReceipt.toString());

        // transaction

        org.web3j.protocol.core.methods.response.Transaction addProductTransaction = this.getTransactionForReceipt(addProductTransactionReceipt);

        BigInteger addProductGasPrice = addProductTransaction.getGasPrice();
        logger.info("addProductTransaction gas price: {}", addProductGasPrice);
        BigInteger addProductGas = addProductTransaction.getGas();
        logger.info("addProductTransaction gas: {}", addProductGas);

        if (product.getGasProvider().getClass().equals(EstimatedGasProvider.class)) {
            //EstimatedContractGasProvider estimatedContractGasProvider = (EstimatedContractGasProvider)productSale.getGasProvider();
            EstimatedGasProvider estimatedGasProvider = (EstimatedGasProvider)product.getGasProvider();
            BigInteger gasLimit = estimatedGasProvider.getLastReturnedGasLimit();
            logger.info("addProductTransaction estimatedGasProvider last returned gas limit: {}", gasLimit);
        }

        // get product id via production validation contract
        // and OwnershipTransferred events

        Flowable<Product.OwnershipTransferredEventResponse> ownershipTransferredEventResponseFlowable = product.ownershipTransferredEventFlowable(DefaultBlockParameter.valueOf(BigInteger.ZERO), DefaultBlockParameterName.LATEST);

        List<Product.OwnershipTransferredEventResponse> ownershipTransferredEventResponseList = new ArrayList<>();
        ownershipTransferredEventResponseFlowable.take(20, TimeUnit.SECONDS)
                .blockingSubscribe(ownershipTransferredEventResponse -> {
                    ownershipTransferredEventResponseList.add(ownershipTransferredEventResponse);
                });

        assertNotEquals(0, ownershipTransferredEventResponseList.size());

        String buyerAddressName = this.productOwner;

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

        // get product from id
        Tuple3<String, String, BigInteger> productInfo = this.getProductFromId(product, productId);

        assertNotNull(productInfo);
        logger.info("{} product info: {}", methodName, productInfo.toString());

        String productOwner = productInfo.component1();
        assertEquals(buyerAddressName, productOwner);

        String productName = productInfo.component2();
        assertEquals(ProductTest.PRODUCT_NAME, productName);

        // is product valid

        Boolean isProductValid = this.validateProduct(product, productId, productName);
        assertTrue(isProductValid);

        // get all product IDs

        List<BigInteger> productIdList = this.getAllProductIds(product);
        assertNotNull(productIdList);

        for (BigInteger productIdInList : productIdList) {
            logger.info("{} product id: {}", methodName, productIdInList);
        }

        assertTrue(productIdList.contains(productId));
    }

    @AfterAll
    public void tearDownTests() {
        logger.info("logBalanceCallCount: {}", this.logBalanceCallCount);
        logger.info("==========");
    }
}