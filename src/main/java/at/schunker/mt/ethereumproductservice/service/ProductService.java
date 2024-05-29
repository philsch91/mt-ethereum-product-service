package at.schunker.mt.ethereumproductservice.service;

import at.schunker.mt.ethereumproductservice.dto.ProductInfo;

import java.math.BigInteger;
import java.util.List;

public interface ProductService {
    public abstract List<BigInteger> getProductIds();
    public abstract ProductInfo getProduct(BigInteger productId);
    public abstract Boolean isProduct(BigInteger productId, String productName);
}
