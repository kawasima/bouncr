package net.unit8.bouncr.util;

public class Base32Utils {
    private static char[] BASE32_CHARS = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z', '2', '3', '4', '5',
            '6', '7' };

    public static String encode(byte[] input) {
        byte b;
        int symbol;
        int carry = 0;
        int shift = 3;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.length; i++) {
            b = input[i];
            symbol = carry | (b >> shift);
            sb.append(BASE32_CHARS[symbol & 0x1f]);

            if (shift > 5) {
                shift -= 5;
                symbol = b >> shift;
                sb.append(BASE32_CHARS[symbol & 0x1f]);
            }

            shift = 5 - shift;
            carry = b << shift;
            shift = 8 - shift;
        }

        if (shift != 3) {
            sb.append(BASE32_CHARS[carry & 0x1f]);
        }

        return sb.toString();
    }
}
