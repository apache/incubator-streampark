package com.streamxhub.console.base.utils;

import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.util.ByteSource;

import java.util.Random;

public class ShaHashUtil {


    /**
     * 用户密码加密
     *
     * @param salt
     * @param password
     * @return
     */
    public static String encrypt(String salt, String password) {
       String pass = new Sha256Hash(password, ByteSource.Util.bytes(salt), 1024).toHex();
        return pass;
    }

    /**
     * 获取随机盐值
     *
     * @param length
     * @return
     */
    public static String getRandomSalt(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String salt = getRandomSalt(26);
        System.out.println(salt);
        String encrypt = encrypt(salt, "123456");
        System.out.println(encrypt);
    }

}
