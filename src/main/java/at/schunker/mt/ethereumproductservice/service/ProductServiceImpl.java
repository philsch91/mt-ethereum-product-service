package at.schunker.mt.ethereumproductservice.service;

import at.schunker.mt.ethereumproductservice.contract.Product;
import at.schunker.mt.ethereumproductservice.dto.ProductInfo;
import at.schunker.mt.ethereumproductservice.tx.EstimatedGasProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tx.gas.StaticGasProvider;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    Web3Service web3Service;
    @Value("${productContractAddress}")
    private String contractAddress;

    public List<BigInteger> getProductIds() {
        //TODO: handle Credentials in Web3Service
        Credentials credentials = Credentials.create(this.web3Service.getPrivateKey());
        System.out.println("privateKey: " + this.web3Service.getPrivateKey());

        EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3Service.getWeb3j());
        //StaticGasProvider gasProvider = new StaticGasProvider(BigInteger.valueOf(20000000000L), BigInteger.valueOf(6721975L));

        //String contractAddress = "0x9F9d520a11BC57086F7b51a6D1158f8AC061E60D";
        String contractAddress = "0x" + this.contractAddress;
        System.out.println("contractAddress: " + contractAddress);

        //Product contract = new Product(this.contractAddress, this.web3j, credentials, gasProvider);
        Product contract = Product.load(contractAddress, this.web3Service.getWeb3j(), credentials, gasProvider);

        RemoteFunctionCall<List> functionCall = contract.getAllProductIds();
        List<BigInteger> productIdList = null;
        try {
            productIdList = functionCall.send();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return null;
        }

        System.out.println("productIdList: " + productIdList);
        for (BigInteger id : productIdList) {
            System.out.println("id: " + id);
        }

        return productIdList;
    }

    public ProductInfo getProduct(BigInteger productId) {
        Credentials credentials = Credentials.create(this.web3Service.getPrivateKey());

        EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3Service.getWeb3j());

        String contractAddress = "0x" + this.contractAddress;

        //Product contract = new Product(this.contractAddress, this.web3j, credentials, gasProvider);
        Product contract = Product.load(contractAddress, this.web3Service.getWeb3j(), credentials, gasProvider);

        RemoteFunctionCall<Tuple3<String, String, BigInteger>> functionCall = contract.getProductFromProductId(productId);
        Tuple3<String, String, BigInteger> product = null;

        try {
            product = functionCall.send();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        System.out.println("product.component1: " + product.component1());
        System.out.println("product.component2: " + product.component2());
        System.out.println("product.component3: " + product.component3().toString());

        ProductInfo productInfo = new ProductInfo();
        productInfo.setOwner(product.component1());
        productInfo.setName(product.component2());
        productInfo.setCreationTimestamp(product.component3());

        return productInfo;
    }

    public Boolean isProduct(BigInteger productId, String productName) {
        Credentials credentials = Credentials.create(this.web3Service.getPrivateKey());

        EstimatedGasProvider gasProvider = new EstimatedGasProvider(this.web3Service.getWeb3j());

        String contractAddress = "0x" + this.contractAddress;

        //Product contract = new Product(this.contractAddress, this.web3j, credentials, gasProvider);
        Product contract = Product.load(contractAddress, this.web3Service.getWeb3j(), credentials, gasProvider);

        RemoteFunctionCall<Boolean> functionCall = contract.isProduct(productId, productName);
        Boolean isProduct = false;
        try {
            isProduct = functionCall.send();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }

        return isProduct;
    }
}
