package at.schunker.mt.ethereumproductservice.service;

import at.schunker.mt.ethereumproductservice.logging.HttpLogger;
import at.schunker.mt.ethereumproductservice.persistence.FileWalletDAO;
import at.schunker.mt.ethereumproductservice.protocol.Web3HttpService;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service("Web3Service")
public class Web3Service implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(Web3Service.class);

    private Web3j web3j;
    @Value("${nodeAddress}")
    private String nodeAddress;
    @Value("${privateKey}")
    private String privateKey;
    @Value("${walletDirectoryPath}")
    private String walletDirectoryPath;

    public Web3Service() {
        // AutoWired
    }

    public Web3j getWeb3j() {
        return this.web3j;
    }

    public String getNodeAddress() {
        return this.nodeAddress;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    @PostConstruct
    public void init() {
        logger.info(this.nodeAddress);
        logger.info(this.privateKey);
        this.web3j = this.build(this.nodeAddress);
        this.getClientVersionSync();

        /*
        String publicKey = CredentialUtils.getPublicKeyInHex(this.privateKey);
        logger.info("publicKey: " + publicKey);

        Credentials cred = Credentials.create(this.privateKey);
        String pk = CredentialUtils.getPublicKeyInCredentials(cred);
        logger.info("publickey: " + pk);
        */

        FileWalletDAO fileWalletDAO = new FileWalletDAO(this.walletDirectoryPath);
        //Credentials credentials = Credentials.create(this.privateKey);

        try {
            fileWalletDAO.saveCredentials(this.privateKey, "Pa$$w0rd");
        } catch (Exception ex) {
            logger.info(ex.getMessage());
            ex.printStackTrace();
        }

        //WalletUtils.loadCredentials("Pa$$w0rd", new File())
    }

    private OkHttpClient buildClient() {
        HttpLogger logger = new HttpLogger();
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(logger);
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                ////.addInterceptor(new LoggingInterceptor())
                .addInterceptor(interceptor)
                .build();

        return client;
    }

    private Web3j build(String nodeAddress) {
        OkHttpClient client = this.buildClient();

        //HttpService httpService = new HttpService(this.nodeAddress, OkHttpClient());
        //HttpService httpService = new HttpService(this.nodeAddress);

        /**
         * If the includeRawResponse argument is passed with the value true
         * an IOException is thrown in the method performIO
         * of the class org.web3j.protocol.http.HttpService
         */
        //HttpService httpService = new HttpService(this.nodeAddress, client, true);
        HttpService httpService = new HttpService(this.nodeAddress, client, false);

        //Web3HttpService httpService = new Web3HttpService(this.nodeAddress, client, false);

        Web3j web3j = Web3j.build(httpService);
        return web3j;
    }

    private void getClientVersionSync() {
        //Web3ClientVersion clientVersion = new Web3ClientVersion();
        Web3ClientVersion clientVersion = null;
        EthAccounts accounts = null;

        Request<?, Web3ClientVersion> request = this.web3j.web3ClientVersion();
        //Request<?, EthAccounts> request = this.web3j.ethAccounts();
        //this.web3j.web3ClientVersion();

        try {
            //request.sendAsync().
            clientVersion = request.send();
            //accounts = request.send();
            //System.out.println(clientVersion.getWeb3ClientVersion());
        } catch (IOException ioex) {
            System.err.println("IOException: " + ioex.getMessage());
            ioex.printStackTrace();
            logger.error("{}", ioex.getMessage());
        } catch (Exception ex) {
            System.err.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
            logger.error("{}", ex.getMessage());
        }

        if (accounts == null) {
            System.err.println("accounts is null");
        }

        //System.out.println(accounts.toString());

        if (clientVersion == null) {
            System.err.println("clientVersion is null");
            throw new RuntimeException("client version is null");
        }

        if (clientVersion.hasError()) {
            System.out.println(clientVersion.getError().toString());
        }

        if (clientVersion.getWeb3ClientVersion() != null) {
            System.out.println("Connected to Ethereum client: " + clientVersion.getWeb3ClientVersion());
        }

        //this.web3j.shutdown();
    }

    /** HealthIndicator */

    @Override
    public Health getHealth(boolean includeDetails) {
        Health health = this.health();

        if (!includeDetails) {
            logger.info(health.getStatus().getCode());
            logger.info(health.getStatus().getDescription());
            //TODO: remove details
        }

        return health;
    }

    @Override
    public Health health() {
        Web3ClientVersion clientVersion = null;
        Request<?, Web3ClientVersion> request = this.web3j.web3ClientVersion();

        try {
            clientVersion = request.send();
        } catch (Exception ex) {
            return Health.down().withException(ex).build();
        }

        if (clientVersion == null) {
            return Health.down().withDetail("ClientVersion is null", 1).build();
        }

        if (clientVersion.hasError()) {
            return Health.down().withDetail(clientVersion.getError().toString(), 1).build();
        }

        String web3ClientVersionName = clientVersion.getWeb3ClientVersion();

        if (web3ClientVersionName == null) {
            return Health.up().build();
        }

        return Health.up().withDetail(web3ClientVersionName, 0).build();
    }
}
