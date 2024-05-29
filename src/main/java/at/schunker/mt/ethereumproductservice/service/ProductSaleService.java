package at.schunker.mt.ethereumproductservice.service;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

public interface ProductSaleService {
    public abstract TransactionReceipt deploy();
    public abstract TransactionReceipt deployContract();
}
