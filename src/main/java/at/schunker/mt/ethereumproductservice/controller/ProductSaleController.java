package at.schunker.mt.ethereumproductservice.controller;

import at.schunker.mt.ethereumproductservice.persistence.DBWalletDAO;
import at.schunker.mt.ethereumproductservice.service.ProductSaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

@Controller
public class ProductSaleController {

    private static final Logger logger = LoggerFactory.getLogger(ProductSaleController.class);
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    ProductSaleService productSaleService;
    @Autowired
    DBWalletDAO dbWalletDAO;

    @RequestMapping(value = "/productsale/deploy")
    public ResponseEntity<TransactionReceipt> deploy() {
        //List<BigInteger> productIdList = this.productService.getProductIds();
        TransactionReceipt transactionReceipt = this.productSaleService.deploy();
        //TransactionReceipt transactionReceipt = this.productSaleService.deployContract();
        if (transactionReceipt == null) {
            ResponseEntity responseEntity = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            return responseEntity;
        }

        ResponseEntity responseEntity = new ResponseEntity<>(transactionReceipt, HttpStatus.CREATED);
        return responseEntity;
    }

    @Deprecated
    @RequestMapping(value = "/wallet")
    public ResponseEntity<String> saveWallet(HttpServletResponse servletResponse) {
        if (this.applicationContext == null) {
            logger.error("saveWallet fails because applicationContext is null and a DI by Spring via a (setter) method annotated with @Autowired" +
                    "or a manual DI from WalletRepository in DBWalletDAO is therefore impossible");
        }
        logger.info("saveWallet applicationContext: {}", this.applicationContext.toString());
        //Credentials credentials = Credentials.create("2dc023d6a5267d130ea5329b8a18180c8e26cfe4a5182fb34623d5db25fcfbb7");
        //Credentials credentials = Credentials.create("acb2cfe8f39bdb84f64acd940ccda4ac2d7639e92c02bc267e663969c05bf3a5");
        String privateKey = "acb2cfe8f39bdb84f64acd940ccda4ac2d7639e92c02bc267e663969c05bf3a5";

        ///*
        try {
            this.dbWalletDAO.saveCredentials(privateKey, "Pa$$w0rd");
        } catch (Exception ex) {
            logger.info(ex.getMessage());
            ex.printStackTrace();
        }//*/

        servletResponse.setHeader("X-Test-Header", "HelloWorld123");
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }
}
