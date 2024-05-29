package at.schunker.mt.ethereumproductservice.util;

import org.web3j.crypto.Bip32ECKeyPair;

import java.nio.ByteBuffer;

import static org.web3j.crypto.Hash.sha256;

public class Bip32Utils {

    static byte[] addChecksum(byte[] input) {
        int inputLength = input.length;
        byte[] checksummed = new byte[inputLength + 4];
        System.arraycopy(input, 0, checksummed, 0, inputLength);
        byte[] checksum = hashTwice(input);
        System.arraycopy(checksum, 0, checksummed, inputLength, 4);
        return checksummed;
    }

    static byte[] serializePublic(Bip32ECKeyPair pair) {
        return serialize(pair, 0x0488B21E, true);
    }

    static byte[] serializePrivate(Bip32ECKeyPair pair) {
        return serialize(pair, 0x0488ADE4, false);
    }

    private static byte[] hashTwice(byte[] input) {
        return sha256(sha256(input));
    }

    private static byte[] serialize(Bip32ECKeyPair pair, int header, boolean pub) {
        ByteBuffer ser = ByteBuffer.allocate(78);
        ser.putInt(header);
        ser.put((byte) pair.getDepth());
        ser.putInt(pair.getParentFingerprint());
        ser.putInt(pair.getChildNumber());
        ser.put(pair.getChainCode());
        ser.put(pub ? pair.getPublicKeyPoint().getEncoded(true) : pair.getPrivateKeyBytes33());
        return ser.array();
    }
}
