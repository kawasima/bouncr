package net.unit8.bouncr.authn;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * HOTP and TOTP Generator.
 *
 * Based on RFC4226
 *
 * @author kawasima
 */
public class OneTimePasswordGenerator {
    private int stepTime = 30;

    public OneTimePasswordGenerator(int stepTime) {
        this.stepTime = stepTime;
    }

    private int dynamicTruncate(byte[] hash) {
    /* DT(String) // String = String[0]...String[19]
     * Let OffsetBits be the low-order 4 bits of String[19]
     * Offset = StToNum(OffsetBits) // 0 <= OffSet <= 15
     * Let P = String[OffSet]...String[OffSet+3]
     * Return the Last 31 bits of P
     */

        int idx = 19;

        int offset =0xf & hash[idx];
        return (hash[offset] & 0x7f) << 24 |
                (hash[offset+1] & 0xff) << 16 |
                (hash[offset+2] & 0xff) << 8 |
                (hash[offset+3] & 0xff);

    }

    public int generateHotp(byte[] key, long counter) {
        HMac hmac = new HMac(new SHA1Digest());
        byte[] hash = new byte[hmac.getMacSize()];
        hmac.init(new KeyParameter(key));
        hmac.update(ByteBuffer.allocate(8).putLong(counter).array(), 0, 8);
        hmac.doFinal(hash, 0);
        return dynamicTruncate(hash) % 1_000_000;
    }

    protected int generateTotp(byte[] key, int generation) {
        int epochTime = (int) (System.currentTimeMillis() / 1000 - generation * stepTime);
        return generateHotp(key, epochTime / stepTime);
    }

    public int generateTotp(byte[] key) {
        return generateTotp(key, 0);
    }

    public Set<Integer> generateTotpSet(byte[] key, int num) {
        return IntStream.rangeClosed(0, num)
                .mapToObj(i -> generateTotp(key, i))
                .collect(Collectors.toSet());
    }
}
