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
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Int8;
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
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple7;
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
public class ProductSale extends BaseContract {
    public static final String BINARY = "6080604052600080546001600160a01b031916331790556115dd806100256000396000f3fe6080604052600436106100e85760003560e01c80638ff2ef3b1161008a578063d4f1259711610059578063d4f1259714610530578063db681e9f14610562578063f3336d601461057f578063fe28524814610594576100e8565b80638ff2ef3b146103725780639670d69914610387578063c3a1e5b31461042f578063c5cc426a1461045b576100e8565b80636ca1608b116100c65780636ca1608b1461025f5780637633a22c14610284578063893d20e81461030e5780638a3ac0971461033f576100e8565b80632a55feec146100ed5780633a23cc0a146101345780635721e4191461019b575b600080fd5b3480156100f957600080fd5b506101206004803603602081101561011057600080fd5b50356001600160a01b03166105b1565b604080519115158252519081900360200190f35b34801561014057600080fd5b5061015e6004803603602081101561015757600080fd5b50356105d6565b60405180856001600160a01b03168152602001848152602001838152602001826001600160a01b0316815260200194505050505060405180910390f35b3480156101a757600080fd5b506101ce600480360360208110156101be57600080fd5b50356001600160a01b031661064f565b60405180846001600160a01b03168152602001806020018315158152602001828103825284818151815260200191508051906020019080838360005b8381101561022257818101518382015260200161020a565b50505050905090810190601f16801561024f5780820380516001836020036101000a031916815260200191505b5094505050505060405180910390f35b6102826004803603604081101561027557600080fd5b5080359060200135610747565b005b34801561029057600080fd5b50610299610a2b565b6040805160208082528351818301528351919283929083019185019080838360005b838110156102d35781810151838201526020016102bb565b50505050905090810190601f1680156103005780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561031a57600080fd5b50610323610a4a565b604080516001600160a01b039092168252519081900360200190f35b34801561034b57600080fd5b506102826004803603602081101561036257600080fd5b50356001600160a01b0316610a59565b34801561037e57600080fd5b50610323610a92565b6102826004803603604081101561039d57600080fd5b8101906020810181356401000000008111156103b857600080fd5b8201836020820111156103ca57600080fd5b803590602001918460018302840111640100000000831117156103ec57600080fd5b91908080601f0160208091040260200160405190810160405280939291908181526020018383808284376000920191909152509295505091359250610aa1915050565b6102826004803603606081101561044557600080fd5b508035906020810135906040013560000b610d7e565b34801561046757600080fd5b506104856004803603602081101561047e57600080fd5b5035610e84565b60405180886001600160a01b0316815260200180602001878152602001868152602001858152602001848152602001838152602001828103825288818151815260200191508051906020019080838360005b838110156104ef5781810151838201526020016104d7565b50505050905090810190601f16801561051c5780820380516001836020036101000a031916815260200191505b509850505050505050505060405180910390f35b6102826004803603606081101561054657600080fd5b50803590602081013590604001356001600160a01b0316610fda565b6102826004803603602081101561057857600080fd5b503561112b565b34801561058b57600080fd5b5061032361123f565b610282600480360360208110156105aa57600080fd5b5035611267565b6001600160a01b03811660009081526005602052604090206002015460ff165b919050565b60008181526004602052604081206002015481908190819060ff166105fa57600080fd5b50505060009182525060046020818152604080842054845260038252808420600e8101546001600160a01b039081168652600590935293205491830154600b8401546007909401549282169490939290911690565b6000805460609082906001600160a01b0316331461066c57600080fd5b610675846105b1565b61067e57600080fd5b6001600160a01b0384811660009081526005602090815260409182902080546002808301546001938401805487516101009682161596909602600019011692909204601f810186900486028501860190965285845291909516949360ff909116928491908301828280156107335780601f1061070857610100808354040283529160200191610733565b820191906000526020600020905b81548152906001019060200180831161071657829003601f168201915b505050505091509250925092509193909250565b60008281526004602052604090206002015460ff1661076557600080fd5b600082815260046020908152604080832080548452600390925290912060078101546001600160a01b0316331461079b57600080fd5b6004810154600e8201546007830154604080518781526001600160a01b0392831660208201528151899493909316927f587b8bd23b43f29f1f6f1d2d4306080dbcc8f78f1ec918c6812b33001fefc990929181900390910190a46000805460058301546040516001600160a01b0390921692839282156108fc029291818181858888f19350505050158015610834573d6000803e3d6000fd5b50600782015460088301546040516001600160a01b0390921691829180156108fc02916000818181858888f19350505050158015610876573d6000803e3d6000fd5b50600e83015483546001600160a01b0319166001600160a01b03918216178455600154166108a75750505050610a27565b600e8301546000906108c1906001600160a01b031661133f565b60018054600280546001600160a01b0319166001600160a01b0392831617908190556040805163ca2efb5560e01b815260048101918252855160448201528551959650919092169363ca2efb55938693908a0192909182916024810191606490910190602087019080838360005b8381101561094757818101518382015260200161092f565b50505050905090810190601f1680156109745780820380516001836020036101000a031916815260200191505b508381038252845460026000196101006001841615020190911604808252602090910190859080156109e75780601f106109bc576101008083540402835291602001916109e7565b820191906000526020600020905b8154815290600101906020018083116109ca57829003601f168201915b5050945050505050600060405180830381600087803b158015610a0957600080fd5b505af1158015610a1d573d6000803e3d6000fd5b5050505050505050505b5050565b604080518082019091526005815264616c69766560d81b602082015290565b6000546001600160a01b031690565b6000546001600160a01b03163314610a7057600080fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b6001546001600160a01b031681565b6040805160608082018352600080835260208084018790524284860152845160e08082018752838252818301849052818701849052818501849052608080830185905260a080840186905260c0808501879052600680546001908101918290558b5161010081018d528b81528089018e9052808d01839052998a01899052938901889052918801859052339088015291860181905290845260038352959092208351805182546001600160a01b0319166001600160a01b0390911617825580830151805196979496929491938593610b7f9385019290910190611506565b5060409182015160029091015560208381015160038401558382015160048401556060808501516005850155608080860151600686015560a08087015180516007880180546001600160a01b039283166001600160a01b03199182161790915595820151600889015595810151600988015592830151600a8701805491871691861691909117905590820151600b860155810151600c85015560c090810151600d8501805491151560ff19909216919091179055840151600e909301805460e0909501511515600160a01b0260ff60a01b1994909316949091169390931791909116179055610c6d33611434565b836040518082805190602001908083835b60208310610c9d5780518252601f199092019160209182019101610c7e565b6001836020036101000a0380198251168184511680821785525050505050509050019150506040518091039020600654336001600160a01b03167f48d915723e47a61b346df51f000b9ad6e9fb32447eeb73edffb1853d1a18258d87876040518080602001838152602001828103825284818151815260200191508051906020019080838360005b83811015610d3d578181015183820152602001610d25565b50505050905090810190601f168015610d6a5780820380516001836020036101000a031916815260200191505b50935050505060405180910390a450505050565b6000546001600160a01b03163314610d9557600080fd5b6000838152600360205260409020600e0154600160a01b900460ff16610dba57600080fd5b8060000b60011480610dcf57508060000b6002145b610dd857600080fd5b8060000b60011415610dfd576000838152600360205260409020600501829055610e21565b600083815260036020526040902060088101839055600d01805460ff191660011790555b6000838152600360209081526040808320600e015481518681529385900b92840192909252805186936001600160a01b03909316927faf54ca7c1ecf75ea1c151f265e9ab3eeecb27d19e8d3ec6b7e199414dd0c1ed692908290030190a3505050565b6000818152600360205260408120600e015460609082908190819081908190600160a01b900460ff16610eb657600080fd5b6000888152600360209081526040808320600e01546001600160a01b03168352600590915290206002015460ff16610eed57600080fd5b600088815260036020818152604092839020600e810154928101546005820154600683015460088401546009850154600195860180548a51600261010099831615999099026000190190911697909704601f8101899004890288018901909a528987526001600160a01b039098169894969395929491939092918891830182828015610fba5780601f10610f8f57610100808354040283529160200191610fba565b820191906000526020600020905b815481529060010190602001808311610f9d57829003601f168201915b505050505095509650965096509650965096509650919395979092949650565b6000838152600360205260409020600e0154600160a01b900460ff16610fff57600080fd5b6000546001600160a01b0316331461101657600080fd5b6000838152600360209081526040808320600e01546001600160a01b031680845260059092529091206002015460ff1661104f57600080fd5b6007805460019081018083556040805160608101825288815260208082018481528284018681526000958652600483528486209351845590519583019590955593516002909101805460ff191691151591909117905587825260038352808220600b8101889055840180546001600160a01b0319166001600160a01b038881169182179092559454868216845260058552928290205482518981529485019590955281518995939493909116927fbc1c01dc0d1e366f76a22e7db8af8028033bd0a521feaf529046a6bc413ce0f092908290030190a450505050565b6000546001600160a01b0316331461114257600080fd5b6000818152600360205260409020600e0154600160a01b900460ff1661116757600080fd5b6000818152600360205260409020600681015460088201546005830154011461118f57600080fd5b600e81015460068201546040516001600160a01b0390921691829180156108fc02916000818181858888f193505050501580156111d0573d6000803e3d6000fd5b506000838152600360209081526040808320600690810193909355600e850154928501548151908152429281019290925280516001600160a01b039093169233927f0ce01a0bcdbcd0d03cf84fcd798760dc1e90610d07a981b68d1d5287bbd3187092908290030190a3505050565b600080546001600160a01b0316331461125757600080fd5b506001546001600160a01b031690565b6000818152600360205260409020600e0154600160a01b900460ff1661128c57600080fd5b6000818152600360205260409020600e01546001600160a01b031633146112b257600080fd5b600081815260036020526040902060088101546005909101540134146112d757600080fd5b34600360008381526020019081526020016000206006018190555080336001600160a01b03167f4ca09f9926d2e48ef31cb6b14dbda230c7420702b8a8f5b878dd572c593e99d23442604051808381526020018281526020019250505060405180910390a350565b60408051602880825260608281019093526000919060208201818036833701905050905060005b601481101561142d5760008160130360080260020a856001600160a01b03168161138c57fe5b0460f81b9050600060108260f81c60ff16816113a457fe5b0460f81b905060008160f81c6010028360f81c0360f81b90506113c6826114d5565b8585600202815181106113d557fe5b60200101906001600160f81b031916908160001a9053506113f5816114d5565b85856002026001018151811061140757fe5b60200101906001600160f81b031916908160001a90535050600190920191506113669050565b5092915050565b61143d816105b1565b15611447576114d2565b604080516060810182526001600160a01b03838116808352835160208082018652600080835281860192835260018688018190529381526005825295909520845181546001600160a01b0319169416939093178355518051939492936114b593928501929190910190611506565b50604091909101516002909101805460ff19169115159190911790555b50565b6000600a60f883901c10156114f5578160f81c60300160f81b90506105d1565b8160f81c60570160f81b90506105d1565b828054600181600116156101000203166002900490600052602060002090601f01602090048101928261153c5760008555611582565b82601f1061155557805160ff1916838001178555611582565b82800160010185558215611582579182015b82811115611582578251825591602001919060010190611567565b5061158e929150611592565b5090565b5b8082111561158e576000815560010161159356fea264697066735822122040ef70981d111de8b509a1ffb3a0306ff002c16c48fea6c92ca3beed05b2800a64736f6c63430007060033";

    public static final String FUNC_DELIVERY = "delivery";

    public static final String FUNC_GETBUYER = "getBuyer";

    public static final String FUNC_GETINVOICE = "getInvoice";

    public static final String FUNC_GETOWNER = "getOwner";

    public static final String FUNC_GETPRODUCTVALIDATIONCONTRACTADDRESS = "getProductValidationContractAddress";

    public static final String FUNC_HEALTH = "health";

    public static final String FUNC_ISBUYER = "isBuyer";

    public static final String FUNC_PRODUCTVALIDATIONCONTRACTADDRESS = "productValidationContractAddress";

    public static final String FUNC_QUERYORDER = "queryOrder";

    public static final String FUNC_SENDINVOICE = "sendInvoice";

    public static final String FUNC_SENDORDER = "sendOrder";

    public static final String FUNC_SENDPRICE = "sendPrice";

    public static final String FUNC_SENDREFUND = "sendRefund";

    public static final String FUNC_SENDSAFEPAY = "sendSafepay";

    public static final String FUNC_SETPRODUCTVALIDATIONCONTRACTADDRESS = "setProductValidationContractAddress";

    public static final Event BUYERREGISTERED_EVENT = new Event("BuyerRegistered", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Utf8String>(true) {}));
    ;

    public static final Event INVOICESENT_EVENT = new Event("InvoiceSent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event ORDERDELIVERED_EVENT = new Event("OrderDelivered", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event ORDERSENT_EVENT = new Event("OrderSent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event PRICESENT_EVENT = new Event("PriceSent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Int8>() {}));
    ;

    public static final Event REFUNDSENT_EVENT = new Event("RefundSent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SAFEPAYSENT_EVENT = new Event("SafepaySent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected ProductSale(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ProductSale(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ProductSale(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ProductSale(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<BuyerRegisteredEventResponse> getBuyerRegisteredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(BUYERREGISTERED_EVENT, transactionReceipt);
        ArrayList<BuyerRegisteredEventResponse> responses = new ArrayList<BuyerRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BuyerRegisteredEventResponse typedResponse = new BuyerRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.name = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<BuyerRegisteredEventResponse> buyerRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, BuyerRegisteredEventResponse>() {
            @Override
            public BuyerRegisteredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(BUYERREGISTERED_EVENT, log);
                BuyerRegisteredEventResponse typedResponse = new BuyerRegisteredEventResponse();
                typedResponse.log = log;
                typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.name = (byte[]) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<BuyerRegisteredEventResponse> buyerRegisteredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BUYERREGISTERED_EVENT));
        return buyerRegisteredEventFlowable(filter);
    }

    public List<InvoiceSentEventResponse> getInvoiceSentEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(INVOICESENT_EVENT, transactionReceipt);
        ArrayList<InvoiceSentEventResponse> responses = new ArrayList<InvoiceSentEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InvoiceSentEventResponse typedResponse = new InvoiceSentEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.invoiceno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.deliveryDate = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.courier = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<InvoiceSentEventResponse> invoiceSentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, InvoiceSentEventResponse>() {
            @Override
            public InvoiceSentEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(INVOICESENT_EVENT, log);
                InvoiceSentEventResponse typedResponse = new InvoiceSentEventResponse();
                typedResponse.log = log;
                typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.invoiceno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
                typedResponse.deliveryDate = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.courier = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<InvoiceSentEventResponse> invoiceSentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INVOICESENT_EVENT));
        return invoiceSentEventFlowable(filter);
    }

    public List<OrderDeliveredEventResponse> getOrderDeliveredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ORDERDELIVERED_EVENT, transactionReceipt);
        ArrayList<OrderDeliveredEventResponse> responses = new ArrayList<OrderDeliveredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderDeliveredEventResponse typedResponse = new OrderDeliveredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.invoiceno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.realDeliveryDate = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.courier = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OrderDeliveredEventResponse> orderDeliveredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OrderDeliveredEventResponse>() {
            @Override
            public OrderDeliveredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ORDERDELIVERED_EVENT, log);
                OrderDeliveredEventResponse typedResponse = new OrderDeliveredEventResponse();
                typedResponse.log = log;
                typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.invoiceno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
                typedResponse.realDeliveryDate = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.courier = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OrderDeliveredEventResponse> orderDeliveredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERDELIVERED_EVENT));
        return orderDeliveredEventFlowable(filter);
    }

    public List<OrderSentEventResponse> getOrderSentEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ORDERSENT_EVENT, transactionReceipt);
        ArrayList<OrderSentEventResponse> responses = new ArrayList<OrderSentEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderSentEventResponse typedResponse = new OrderSentEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.indexedProductName = (byte[]) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.productName = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.quantity = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OrderSentEventResponse> orderSentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OrderSentEventResponse>() {
            @Override
            public OrderSentEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ORDERSENT_EVENT, log);
                OrderSentEventResponse typedResponse = new OrderSentEventResponse();
                typedResponse.log = log;
                typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.indexedProductName = (byte[]) eventValues.getIndexedValues().get(2).getValue();
                typedResponse.productName = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.quantity = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OrderSentEventResponse> orderSentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERSENT_EVENT));
        return orderSentEventFlowable(filter);
    }

    public List<PriceSentEventResponse> getPriceSentEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(PRICESENT_EVENT, transactionReceipt);
        ArrayList<PriceSentEventResponse> responses = new ArrayList<PriceSentEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PriceSentEventResponse typedResponse = new PriceSentEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.price = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.ttype = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<PriceSentEventResponse> priceSentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, PriceSentEventResponse>() {
            @Override
            public PriceSentEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(PRICESENT_EVENT, log);
                PriceSentEventResponse typedResponse = new PriceSentEventResponse();
                typedResponse.log = log;
                typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.price = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.ttype = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<PriceSentEventResponse> priceSentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PRICESENT_EVENT));
        return priceSentEventFlowable(filter);
    }

    public List<RefundSentEventResponse> getRefundSentEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REFUNDSENT_EVENT, transactionReceipt);
        ArrayList<RefundSentEventResponse> responses = new ArrayList<RefundSentEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RefundSentEventResponse typedResponse = new RefundSentEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse._from = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse._to = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse._value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse._timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RefundSentEventResponse> refundSentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, RefundSentEventResponse>() {
            @Override
            public RefundSentEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REFUNDSENT_EVENT, log);
                RefundSentEventResponse typedResponse = new RefundSentEventResponse();
                typedResponse.log = log;
                typedResponse._from = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse._to = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse._value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse._timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<RefundSentEventResponse> refundSentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REFUNDSENT_EVENT));
        return refundSentEventFlowable(filter);
    }

    public List<SafepaySentEventResponse> getSafepaySentEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SAFEPAYSENT_EVENT, transactionReceipt);
        ArrayList<SafepaySentEventResponse> responses = new ArrayList<SafepaySentEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SafepaySentEventResponse typedResponse = new SafepaySentEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.now = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SafepaySentEventResponse> safepaySentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SafepaySentEventResponse>() {
            @Override
            public SafepaySentEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SAFEPAYSENT_EVENT, log);
                SafepaySentEventResponse typedResponse = new SafepaySentEventResponse();
                typedResponse.log = log;
                typedResponse.buyer = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.orderno = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.now = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SafepaySentEventResponse> safepaySentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SAFEPAYSENT_EVENT));
        return safepaySentEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> delivery(BigInteger invoiceno, BigInteger timestamp) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DELIVERY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceno), 
                new org.web3j.abi.datatypes.generated.Uint256(timestamp)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple3<String, String, Boolean>> getBuyer(String _buyerAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETBUYER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _buyerAddress)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple3<String, String, Boolean>>(function,
                new Callable<Tuple3<String, String, Boolean>>() {
                    @Override
                    public Tuple3<String, String, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, String, Boolean>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (Boolean) results.get(2).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Tuple4<String, BigInteger, BigInteger, String>> getInvoice(BigInteger invoiceno) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETINVOICE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(invoiceno)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}));
        return new RemoteFunctionCall<Tuple4<String, BigInteger, BigInteger, String>>(function,
                new Callable<Tuple4<String, BigInteger, BigInteger, String>>() {
                    @Override
                    public Tuple4<String, BigInteger, BigInteger, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<String, BigInteger, BigInteger, String>(
                                (String) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (String) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<String> getOwner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> getProductValidationContractAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETPRODUCTVALIDATIONCONTRACTADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> health() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_HEALTH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> isBuyer(String _address) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISBUYER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _address)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> productValidationContractAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PRODUCTVALIDATIONCONTRACTADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> queryOrder(BigInteger orderno) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_QUERYORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderno)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple7<String, String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (BigInteger) results.get(5).getValue(), 
                                (BigInteger) results.get(6).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> sendInvoice(BigInteger orderno, BigInteger deliveryDate, String courier) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SENDINVOICE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderno), 
                new org.web3j.abi.datatypes.generated.Uint256(deliveryDate), 
                new org.web3j.abi.datatypes.Address(160, courier)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> sendOrder(String productName, BigInteger quantity) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SENDORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(productName), 
                new org.web3j.abi.datatypes.generated.Uint256(quantity)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> sendPrice(BigInteger orderno, BigInteger price, BigInteger ttype) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SENDPRICE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderno), 
                new org.web3j.abi.datatypes.generated.Uint256(price), 
                new org.web3j.abi.datatypes.generated.Int8(ttype)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> sendRefund(BigInteger _orderNumber) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SENDREFUND, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_orderNumber)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> sendSafepay(BigInteger orderno) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SENDSAFEPAY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderno)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    /**
     * Manually added
     * Web3j Maven plugin does not support msg.value in contract function
     * @param orderno
     * @param weiValue
     * @return
     */
    public RemoteFunctionCall<TransactionReceipt> sendSafepay(BigInteger orderno, BigInteger weiValue) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SENDSAFEPAY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(orderno)),
                Collections.<TypeReference<?>>emptyList());
        return this.executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> setProductValidationContractAddress(String contractAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETPRODUCTVALIDATIONCONTRACTADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, contractAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ProductSale load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ProductSale(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ProductSale load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ProductSale(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ProductSale load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ProductSale(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ProductSale load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ProductSale(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ProductSale> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ProductSale.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<ProductSale> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(ProductSale.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<ProductSale> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ProductSale.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<ProductSale> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(ProductSale.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class BuyerRegisteredEventResponse extends BaseEventResponse {
        public String buyer;

        public byte[] name;
    }

    public static class InvoiceSentEventResponse extends BaseEventResponse {
        public String buyer;

        public BigInteger invoiceno;

        public BigInteger orderno;

        public BigInteger deliveryDate;

        public String courier;
    }

    public static class OrderDeliveredEventResponse extends BaseEventResponse {
        public String buyer;

        public BigInteger invoiceno;

        public BigInteger orderno;

        public BigInteger realDeliveryDate;

        public String courier;
    }

    public static class OrderSentEventResponse extends BaseEventResponse {
        public String buyer;

        public BigInteger orderno;

        public byte[] indexedProductName;

        public String productName;

        public BigInteger quantity;
    }

    public static class PriceSentEventResponse extends BaseEventResponse {
        public String buyer;

        public BigInteger orderno;

        public BigInteger price;

        public BigInteger ttype;
    }

    public static class RefundSentEventResponse extends BaseEventResponse {
        public String _from;

        public String _to;

        public BigInteger _value;

        public BigInteger _timestamp;
    }

    public static class SafepaySentEventResponse extends BaseEventResponse {
        public String buyer;

        public BigInteger orderno;

        public BigInteger value;

        public BigInteger now;
    }
}
