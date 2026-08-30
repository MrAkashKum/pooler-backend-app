package com.akash.pooler_backend.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;

public final class JwtEcSignatureUtil {

    private static final int ES256_SIGNATURE_LENGTH = 64;
    private static final int ES256_COMPONENT_LENGTH = 32;

    private JwtEcSignatureUtil() {
    }

    public static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    public static boolean verifyEs256(String signingInput, String signature, PublicKey publicKey)
            throws GeneralSecurityException {
        byte[] rawSignature = base64UrlDecode(signature);
        if (rawSignature.length != ES256_SIGNATURE_LENGTH) {
            return false;
        }

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(publicKey);
        verifier.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return verifier.verify(rawToDer(rawSignature));
    }

    private static byte[] rawToDer(byte[] rawSignature) {
        byte[] r = derInteger(Arrays.copyOfRange(rawSignature, 0, ES256_COMPONENT_LENGTH));
        byte[] s = derInteger(Arrays.copyOfRange(rawSignature, ES256_COMPONENT_LENGTH, ES256_SIGNATURE_LENGTH));
        int sequenceLength = r.length + s.length;
        byte[] der = new byte[sequenceLength + 2];
        der[0] = 0x30;
        der[1] = (byte) sequenceLength;
        System.arraycopy(r, 0, der, 2, r.length);
        System.arraycopy(s, 0, der, 2 + r.length, s.length);
        return der;
    }

    private static byte[] derInteger(byte[] rawInteger) {
        byte[] unsigned = new BigInteger(1, rawInteger).toByteArray();
        byte[] der = new byte[unsigned.length + 2];
        der[0] = 0x02;
        der[1] = (byte) unsigned.length;
        System.arraycopy(unsigned, 0, der, 2, unsigned.length);
        return der;
    }
}
