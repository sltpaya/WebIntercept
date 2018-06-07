package com.sltpaya.open.web.processor;

import java.util.Random;

/**
 * Author: SLTPAYA
 * Date: 2018/4/3
 */
class StringUtils {

    static boolean isNotEmpty(CharSequence input) {
        return input != null && input.length() > 0;
    }

    static String getRandomStrWithFieldName() {
        String str = getRandomString(5);
        char c = Character.toUpperCase(str.charAt(0));
        return String.valueOf(c) + str.substring(1, str.length());
    }

    private static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            int number = random.nextInt(26);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

}
