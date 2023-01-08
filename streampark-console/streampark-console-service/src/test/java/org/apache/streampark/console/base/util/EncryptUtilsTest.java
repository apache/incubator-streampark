package org.apache.streampark.console.base.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EncryptUtilsTest {

    @Test
    public void testEncrypt() throws Exception {
        String value = "apache streampark";
        String encrypt = EncryptUtils.encrypt(value, "streampark");
        String decrypt = EncryptUtils.decrypt(encrypt, "streampark");
        Assertions.assertEquals(value, decrypt);
    }
}
