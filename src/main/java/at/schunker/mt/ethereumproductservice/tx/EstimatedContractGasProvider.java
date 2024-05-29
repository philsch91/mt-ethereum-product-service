package at.schunker.mt.ethereumproductservice.tx;

import org.web3j.tx.gas.ContractGasProvider;

import java.io.IOException;
import java.math.BigInteger;

public interface EstimatedContractGasProvider extends ContractGasProvider {
    BigInteger getGasLimit(String fromAddress, BigInteger gasPrice,
                           String contractAddress, BigInteger weiValue, String data) throws IOException;
}
