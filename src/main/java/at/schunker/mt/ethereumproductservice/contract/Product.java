package at.schunker.mt.ethereumproductservice.contract;

import at.schunker.mt.ethereumproductservice.tx.BaseContract;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.6.4.
 */
@SuppressWarnings("rawtypes")
public class Product extends BaseContract {
    public static final String BINARY = "608060405234801561001057600080fd5b50600080546001600160a01b0319163317815542600255600355610a4f806100396000396000f3fe608060405234801561001057600080fd5b50600436106100935760003560e01c8063c17c7cbb11610066578063c17c7cbb14610138578063ca2efb55146101f9578063d6c7bdbe14610326578063da3fefe414610428578063e6bbbec31461044e57610093565b806343d726d6146100985780634a348da9146100a2578063663c01af146100bc5780636b6731f614610114575b600080fd5b6100a0610456565b005b6100aa61047b565b60408051918252519081900360200190f35b6100c4610481565b60408051602080825283518183015283519192839290830191858101910280838360005b838110156101005781810151838201526020016100e8565b505050509050019250505060405180910390f35b61011c6104f1565b604080516001600160a01b039092168252519081900360200190f35b6101e56004803603604081101561014e57600080fd5b8135919081019060408101602082013564010000000081111561017057600080fd5b82018360208201111561018257600080fd5b803590602001918460018302840111640100000000831117156101a457600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610500945050505050565b604080519115158252519081900360200190f35b6100a06004803603604081101561020f57600080fd5b81019060208101813564010000000081111561022a57600080fd5b82018360208201111561023c57600080fd5b8035906020019184600183028401116401000000008311171561025e57600080fd5b91908080601f01602080910402602001604051908101604052809392919081815260200183838082843760009201919091525092959493602081019350359150506401000000008111156102b157600080fd5b8201836020820111156102c357600080fd5b803590602001918460018302840111640100000000831117156102e557600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610617945050505050565b6103436004803603602081101561033c57600080fd5b50356107d3565b604051808060200180602001848152602001838103835286818151815260200191508051906020019080838360005b8381101561038a578181015183820152602001610372565b50505050905090810190601f1680156103b75780820380516001836020036101000a031916815260200191505b50838103825285518152855160209182019187019080838360005b838110156103ea5781810151838201526020016103d2565b50505050905090810190601f1680156104175780820380516001836020036101000a031916815260200191505b509550505050505060405180910390f35b6100a06004803603602081101561043e57600080fd5b50356001600160a01b0316610917565b61011c610950565b6000546001600160a01b0316331461046d57600080fd5b6000546001600160a01b0316ff5b60045490565b6000546060906001600160a01b0316331461049b57600080fd5b60048054806020026020016040519081016040528092919081815260200182805480156104e757602002820191906000526020600020905b8154815260200190600101908083116104d3575b5050505050905090565b6001546001600160a01b031681565b6000816040516020018082805190602001908083835b602083106105355780518252601f199092019160209182019101610516565b6001836020036101000a038019825116818451168082178552505050505050905001915050604051602081830303815290604052805190602001206005600085815260200190815260200160002060010160405160200180828054600181600116156101000203166002900480156105e45780601f106105c25761010080835404028352918201916105e4565b820191906000526020600020905b8154815290600101906020018083116105d0575b505091505060405160208183030381529060405280519060200120141561060d57506001610611565b5060005b92915050565b60025442101561062657600080fd5b6000546001600160a01b031633148061064957506001546001600160a01b031633145b61065257600080fd5b60035460010160009081526005602090815260409091208351909161067b918391860190610978565b5081516106919060018301906020850190610978565b504260028201556003805460048054600181810183556000929092529181017f8a35acfbc15ff81a39ae7d344fd709f28e8600b4aa8c65c6b64bfe7fe36bd19b90920191909155815401908190556040518451859190819060208401908083835b602083106107115780518252601f1990920191602091820191016106f2565b51815160209384036101000a6000190180199092169116179052604080519290940182900382208183528a51838301528a519096507fb0c908551cca4c8e0e5e69ffebb2b04fbb5f747106fc89eb4f256a93d26a76b795508a94929350839283019185019080838360005b8381101561079457818101518382015260200161077c565b50505050905090810190601f1680156107c15780820380516001836020036101000a031916815260200191505b509250505060405180910390a3505050565b600081815260056020908152604080832060028082015482548451600180831615610100026000190190921693909304601f810187900487028401870190955284835260609687969095918501939185918301828280156108755780601f1061084a57610100808354040283529160200191610875565b820191906000526020600020905b81548152906001019060200180831161085857829003601f168201915b5050855460408051602060026001851615610100026000190190941693909304601f8101849004840282018401909252818152959850879450925084019050828280156109035780601f106108d857610100808354040283529160200191610903565b820191906000526020600020905b8154815290600101906020018083116108e657829003601f168201915b505050505091509250925092509193909250565b6000546001600160a01b0316331461092e57600080fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b600080546001600160a01b0316331461096857600080fd5b506001546001600160a01b031690565b828054600181600116156101000203166002900490600052602060002090601f0160209004810192826109ae57600085556109f4565b82601f106109c757805160ff19168380011785556109f4565b828001600101855582156109f4579182015b828111156109f45782518255916020019190600101906109d9565b50610a00929150610a04565b5090565b5b80821115610a005760008155600101610a0556fea264697066735822122034ebd4197ef43809d5a507e7133576ece9375475aa1b79e4cd0c335ca1b6f8eb64736f6c63430007060033";

    public static final String FUNC_ADDPRODUCT = "addProduct";

    public static final String FUNC_CLOSE = "close";

    public static final String FUNC_GETALLPRODUCTIDS = "getAllProductIds";

    public static final String FUNC_GETPRODUCTCOUNT = "getProductCount";

    public static final String FUNC_GETPRODUCTFROMPRODUCTID = "getProductFromProductId";

    public static final String FUNC_GETPRODUCTSALECONTRACTADDRESS = "getProductSaleContractAddress";

    public static final String FUNC_ISPRODUCT = "isProduct";

    public static final String FUNC_PRODUCTSALECONTRACTADDRESS = "productSaleContractAddress";

    public static final String FUNC_SETPRODUCTSALECONTRACTADDRESS = "setProductSaleContractAddress";

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event RETURNVALUE_EVENT = new Event("ReturnValue", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected Product(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Product(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Product(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Product(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<OwnershipTransferredEventResponse> responses = new ArrayList<OwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.indexedProductOwnerName = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.productId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.productOwnerName = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OwnershipTransferredEventResponse>() {
            @Override
            public OwnershipTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
                OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.indexedProductOwnerName = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.productId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.productOwnerName = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT));
        return ownershipTransferredEventFlowable(filter);
    }

    public List<ReturnValueEventResponse> getReturnValueEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(RETURNVALUE_EVENT, transactionReceipt);
        ArrayList<ReturnValueEventResponse> responses = new ArrayList<ReturnValueEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ReturnValueEventResponse typedResponse = new ReturnValueEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.productOwnerName = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.productName = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.creationDate = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ReturnValueEventResponse> returnValueEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ReturnValueEventResponse>() {
            @Override
            public ReturnValueEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(RETURNVALUE_EVENT, log);
                ReturnValueEventResponse typedResponse = new ReturnValueEventResponse();
                typedResponse.log = log;
                typedResponse.productOwnerName = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.productName = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.creationDate = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ReturnValueEventResponse> returnValueEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RETURNVALUE_EVENT));
        return returnValueEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addProduct(String _productOwnerName, String _productName) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ADDPRODUCT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_productOwnerName), 
                new org.web3j.abi.datatypes.Utf8String(_productName)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> close() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLOSE, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<List> getAllProductIds() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETALLPRODUCTIDS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> getProductCount() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETPRODUCTCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple3<String, String, BigInteger>> getProductFromProductId(BigInteger _productId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETPRODUCTFROMPRODUCTID, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_productId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple3<String, String, BigInteger>>(function,
                new Callable<Tuple3<String, String, BigInteger>>() {
                    @Override
                    public Tuple3<String, String, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, String, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue());
                    }
                });
    }

    public RemoteFunctionCall<String> getProductSaleContractAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETPRODUCTSALECONTRACTADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> isProduct(BigInteger _productId, String _productName) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISPRODUCT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_productId), 
                new org.web3j.abi.datatypes.Utf8String(_productName)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> productSaleContractAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PRODUCTSALECONTRACTADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setProductSaleContractAddress(String contractAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETPRODUCTSALECONTRACTADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, contractAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static Product load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Product(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Product load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Product(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Product load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Product(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Product load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Product(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Product> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Product.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<Product> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Product.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Product> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Product.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Product> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Product.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public byte[] indexedProductOwnerName;

        public BigInteger productId;

        public String productOwnerName;
    }

    public static class ReturnValueEventResponse extends BaseEventResponse {
        public String productOwnerName;

        public String productName;

        public BigInteger creationDate;
    }
}
