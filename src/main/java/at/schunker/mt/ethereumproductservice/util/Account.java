package at.schunker.mt.ethereumproductservice.util;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthGetBalance;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Account {

    public static EthAccounts getEthAccounts(Web3j web3j) {
        EthAccounts accounts = null;
        try {
            accounts =  web3j.ethAccounts().send();
        } catch (IOException ioex) {
            System.err.println(ioex.getMessage());
        }
        return accounts;
    }

    public static EthAccounts getEthAccountsAsync(Web3j web3j) {
        EthAccounts accounts = new EthAccounts();
        try {
            accounts = web3j.ethAccounts()
                    .sendAsync()
                    .get();
        } catch (ExecutionException execex) {
            System.err.println(execex);
        } catch (InterruptedException intex) {
            System.err.println(intex);
        }
        return accounts;
    }

    public static EthGetBalance getEthBalance(Web3j web3j, String address) {
        EthGetBalance result = new EthGetBalance();
        try {
            web3j.ethGetBalance(address, DefaultBlockParameter.valueOf("latest")).send();
        } catch (IOException ioex) {
            System.err.println(ioex.getMessage());
        }
        return result;
    }
}
