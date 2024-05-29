package at.schunker.mt.ethereumproductservice.data;

import org.web3j.crypto.WalletFile;

import java.util.Optional;

public interface WalletRepositoryExtension {
    public abstract Optional<WalletFile> findByAddress(String address);
}
