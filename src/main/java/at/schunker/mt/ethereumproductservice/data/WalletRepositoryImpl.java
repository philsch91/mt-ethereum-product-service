package at.schunker.mt.ethereumproductservice.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.web3j.crypto.WalletFile;

import java.util.List;
import java.util.Optional;

public class WalletRepositoryImpl implements WalletRepositoryExtension {

    private static final Logger logger = LoggerFactory.getLogger(WalletRepositoryImpl.class);
    @Autowired
    WalletRepository walletRepository;

    @Override
    public Optional<WalletFile> findByAddress(String address) {
        List<WalletFile> walletFileList = this.walletRepository.findAll();

        if (walletFileList == null) {
            return null;
        }

        for (WalletFile walletFile : walletFileList) {
            if (walletFile.getAddress().equals(address)) {
                return Optional.of(walletFile);
            }
        }

        return Optional.empty();
    }
}
