package at.schunker.mt.ethereumproductservice.dto;

import org.web3j.tuples.generated.Tuple3;

import java.math.BigInteger;

public class ProductInfo {
    private String name;
    private String owner;
    private BigInteger creationTimestamp;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public BigInteger getCreationTimestamp() {
        return this.creationTimestamp;
    }

    public void setCreationTimestamp(BigInteger creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public static ProductInfo convertFromTuple3(Tuple3<String, String, BigInteger> productTuple) {
        ProductInfo productInfo = new ProductInfo();
        productInfo.owner = productTuple.component1();
        productInfo.name = productTuple.component2();
        productInfo.creationTimestamp = productTuple.component3();

        return productInfo;
    }

    @Override
    public String toString() {
        return "ProductInformation{" +
                "name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                '}';
    }
}
