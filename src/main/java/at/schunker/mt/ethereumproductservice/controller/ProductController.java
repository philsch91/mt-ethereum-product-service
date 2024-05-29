package at.schunker.mt.ethereumproductservice.controller;

import at.schunker.mt.ethereumproductservice.dto.ProductInfo;
import at.schunker.mt.ethereumproductservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;

@Controller
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    @Autowired
    ProductService productService;

    @RequestMapping(value = "/productids")
    public ResponseEntity<List<BigInteger>> getProductIds() {
        List<BigInteger> productIdList = this.productService.getProductIds();
        ResponseEntity responseEntity = new ResponseEntity<>(productIdList, HttpStatus.OK);
        return responseEntity;
    }

    @RequestMapping(value = "/product/{id}", method = RequestMethod.GET)
    public ResponseEntity<ProductInfo> getProduct(@PathVariable("id") String id) {
        BigInteger productId = BigInteger.valueOf(Long.parseLong(id));
        ProductInfo productInfo = this.productService.getProduct(productId);
        HttpStatus status = HttpStatus.OK;
        if (productInfo == null) {
            status = HttpStatus.NOT_FOUND;
        }
        ResponseEntity responseEntity = new ResponseEntity<>(productInfo, status);
        return responseEntity;
    }

    @RequestMapping(value = "/product", method = RequestMethod.GET)
    public ResponseEntity<Boolean> isProduct(@RequestParam("id") long id, @RequestParam("name") String name) {
        boolean isProduct = this.productService.isProduct(BigInteger.valueOf(id), name);
        ResponseEntity responseEntity = new ResponseEntity<>(isProduct, HttpStatus.OK);
        return responseEntity;
    }

    @MessageMapping("/product")
    @SendTo("/topic/products")
    public ProductInfo test(ProductInfo product) throws Exception {
        logger.info(product.getName());
        ProductInfo productInfo = new ProductInfo();
        productInfo.setName(product.getName());
        productInfo.setOwner("Philipp");
        productInfo.setCreationTimestamp(new BigInteger("1614762571"));
        logger.info(productInfo.toString());
        return productInfo;
    }
}
