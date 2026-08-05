package org.fnm.helper;

import com.auth0.jwt.algorithms.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

public class AlgorithmHelper {

    public static Algorithm loadECPrivateKey() throws ParseException, IOException, JOSEException {
        String privateKeyAsString = readResource("JWK-EC-pair.json");
        JWK privateJWK = JWK.parse(privateKeyAsString);
        ECKey privateKey = privateJWK.toECKey();
        return Algorithm.ECDSA256(privateKey.toECPrivateKey());
    }

    public static Algorithm loadECPublicKey() throws ParseException, IOException, JOSEException {
        String publicKeyAsString =  readResource("JWK-EC-public-key.json");
        JWK publicJWK = JWK.parse(publicKeyAsString);
        ECKey publicKey = publicJWK.toECKey();
        return Algorithm.ECDSA256(publicKey.toECPublicKey());
    }

    public static Algorithm loadRSAPrivateKey() throws ParseException, IOException, JOSEException {
        String privateKeyAsString =  readResource("JWK-RSA-pair.json");
        JWK privateJWK = JWK.parse(privateKeyAsString);
        RSAKey privateKey = privateJWK.toRSAKey();
        return Algorithm.RSA256(privateKey.toRSAPrivateKey());
    }

    public static Algorithm loadRSAPublicKey() throws ParseException, IOException, JOSEException {
        String publicKeyAsString =  readResource("JWK-RSA-public-key.json");
        JWK publicJWK = JWK.parse(publicKeyAsString);
        RSAKey publicKey = publicJWK.toRSAKey();
        return Algorithm.RSA256(publicKey.toRSAPublicKey());
    }


    private static String readResource(String fileName) {
        try (InputStream inputStream = AlgorithmHelper.class
                .getClassLoader()
                .getResourceAsStream(fileName)) {

            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + fileName);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + fileName, e);
        }
    }
}
