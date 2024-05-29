package at.schunker.mt.ethereumproductservice.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.web3j.crypto.WalletFile;

public interface WalletRepository extends MongoRepository<WalletFile, String>, WalletRepositoryExtension {
    //
}
