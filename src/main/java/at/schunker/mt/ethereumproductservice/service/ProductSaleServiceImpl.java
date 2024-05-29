package at.schunker.mt.ethereumproductservice.service;

import at.schunker.mt.ethereumproductservice.contract.ProductSale;
import at.schunker.mt.ethereumproductservice.tx.EstimatedGasProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import javax.annotation.Resource;
import java.math.BigInteger;

@Service
public class ProductSaleServiceImpl implements ProductSaleService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Resource
    Web3Service web3Service;
    @Value("${productSaleContractAddress:#{null}}")
    private String contractAddress;

    public TransactionReceipt deploy() {
        //TODO: handle Credentials in Web3Service
        Credentials credentials = Credentials.create(this.web3Service.getPrivateKey());
        System.out.println("privateKey: " + this.web3Service.getPrivateKey());

        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));
        EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3Service.getWeb3j());
        // ProductSale.BINARY
        // new Transaction(from, nonce, gasPrice, gasLimit, to, value, data)
        Transaction transaction = new Transaction(null, BigInteger.ZERO, BigInteger.ZERO,
                gasProvider.getDefaultGasLimit(), null, BigInteger.ZERO, ProductSale.BINARY);
        gasProvider.setTransaction(transaction);

        RemoteCall<ProductSale> deploymentRemoteCall = ProductSale.deploy(this.web3Service.getWeb3j(), credentials, gasProvider);
        ProductSale saleContract;
        TransactionReceipt transactionReceipt;
        try {
            saleContract = deploymentRemoteCall.send();
            //saleContract.setGasPrice(); // use contract.setGasProvider();
            //saleContract.getGasPrice()
            //saleContract.setGasProvider();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return null;
        }
        this.contractAddress = saleContract.getContractAddress();

        if (!saleContract.getTransactionReceipt().isPresent()) {
            return null;
        }
        transactionReceipt = saleContract.getTransactionReceipt().get();
        return transactionReceipt;
    }

    @SuppressWarnings({"unchecked", "deprecated", "SpringElInspection"})
    public TransactionReceipt deployContract() {
        Credentials credentials = Credentials.create(this.web3Service.getPrivateKey());
        System.out.println("privateKey: " + this.web3Service.getPrivateKey());

        EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3Service.getWeb3j());
        TransactionReceipt transactionReceipt = null;
        try {
            //transactionReceipt = ProductSale.deployContract(this.web3Service.getWeb3j(), credentials, gasProvider);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return null;
        }

        //System.out.println("transaction hash: " + transactionReceipt.getTransactionHash());
        //System.out.println("transaction gas: " + transactionReceipt.getGasUsed());

        //this.contractAddress = transactionReceipt.getContractAddress();
        logger.info("contact address: " + this.contractAddress);

        return transactionReceipt;
    }
}
