package id.wit.nfctest;


public class ParseHelper {
    private static final byte[] HEX_CHAR_TABLE = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70};


    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[(len / 2)];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String getHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public static String getHexString(byte[] raw, int len) {
        byte[] hex = new byte[(len * 2)];
        int pos = 0;
        int index = 0;
        for (byte b : raw) {
            if (pos >= len) {
                break;
            }
            pos++;
            int v = b & 255;
            int index2 = index + 1;
            hex[index] = HEX_CHAR_TABLE[v >>> 4];
            index = index2 + 1;
            hex[index2] = HEX_CHAR_TABLE[v & 15];
        }
        return new String(hex);
    }

    public static String getHexStringoff(byte[] raw, int offStart, int len) {
        byte[] hex = new byte[(len * 2)];
        if (offStart + len >= raw.length) {
            return new String(hex);
        }
        int index = 0;
        while (index < len) {
            int v = raw[offStart + index] & 255;
            int index2 = index + 1;
            hex[index] = HEX_CHAR_TABLE[v >>> 4];
            int index3 = index2 + 1;
            hex[index2] = HEX_CHAR_TABLE[v & 15];
            index = index3 + 1;
        }
        int i = index;
        return new String(hex);
    }

    public static String getHex2Char(byte[] raw, int len) {
        int index;
        byte[] hex = new byte[len];
        int pos = 0;
        int length = raw.length;
        int i = 0;
        int index2 = 0;
        while (i < length) {
            byte b = raw[i];
            if (pos >= len) {
                break;
            }
            pos++;
            int v = b & 255;
            if (v < 32) {
                index = index2 + 1;
                hex[index2] = 46;
            } else if (v > 127) {
                index = index2 + 1;
                hex[index2] = 46;
            } else {
                index = index2 + 1;
                hex[index2] = b;
            }
            i++;
            index2 = index;
        }
        return new String(hex);
    }
    public static int fromLittleEndian(final String hex) {
        int ret = 0;
        String hexLittleEndian = "";
        if (hex.length() % 2 != 0) return ret;
        for (int i = hex.length() - 2; i >= 0; i -= 2) {
            hexLittleEndian += hex.substring(i, i + 2);
        }
        ret = Integer.parseInt(hexLittleEndian, 16);
        return ret;
    }
    public static int hexToDecimal(String str) {
        String str2 = "0123456789ABCDEF";
        String upperCase = str.replaceAll(" ", "").toUpperCase();
        int i = 0;
        for (int i2 = 0; i2 < upperCase.length(); i2++) {
            i = (i * 16) + str2.indexOf(upperCase.charAt(i2));
        }
        return i;
    }

    public static boolean isValid(byte[] bArr) {
        byte b2 = bArr[bArr.length - 2];
        byte b3 = bArr[bArr.length - 1];
        boolean z = false;
        boolean z2 = b2 == -112 || b2 == -111;
        if (b3 == 0) {
            z = true;
        }
        return z2 & z;
    }

    public static String getHexString(byte[] bArr) {
        return getHexString(bArr, bArr.length);
    }

    private static int getChar(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        char c2 = 'A';
        if (c < 'A' || c > 'F') {
            c2 = 'a';
            if (c < 'a' || c > 'f') {
                return 0;
            }
        }
        return (c - c2) + 10;
    }


    private static String getTipe(int t) {
        if (t == 288) {
            return "Payment";
        } else if (t == 256 || t == 336) {
            return "Topup";
        } else {
            return "Unknown";
        }
    }
    
    public static String getHexString(String str) {
        StringBuilder sb = new StringBuilder();
        byte[] d = getByteFromStr(str);
        for (byte b2 : d) {
            if ((char) b2 != ' ') {
                sb.append((char) b2);
            }

        }
        return sb.toString();
    }

    private static byte[] getByteFromStr(String str) {
        int length = str.length();
        byte[] bArr = new byte[((length + 1) / 2)];
        int i = 0;
        int i2 = 1;
        if (length % 2 == 1) {
            bArr[0] = (byte) getChar(str.charAt(0));
            i = 1;
        } else {
            i2 = 0;
        }
        while (i < length) {
            int i3 = i2 + 1;
            int i4 = i + 1;
            int i5 = i4 + 1;
            bArr[i2] = (byte) ((getChar(str.charAt(i)) << 4) | getChar(str.charAt(i4)));
            i2 = i3;
            i = i5;
        }
        return bArr;
    }

    public static String m44c(String str) {
        StringBuilder sb = new StringBuilder();
        int parseInt = Integer.parseInt(str, 16);
        int i = parseInt / 86400;
        StringBuilder sb2 = new StringBuilder();
        int i2 = i + 2444240 + 68569;
        int i3 = (i2 * 4) / 146097;
        int i4 = i2 - (((146097 * i3) + 3) / 4);
        int i5 = ((i4 + 1) * 4000) / 1461001;
        int i6 = (i4 - ((i5 * 1461) / 4)) + 31;
        int i7 = (i6 * 80) / 2447;
        int i8 = i6 - ((i7 * 2447) / 80);
        int i9 = i7 / 11;
        int i10 = (i7 + 2) - (i9 * 12);
        Object[] objArr = {Integer.valueOf(((((i3 - 49) * 100) + i5) + i9) % 100)};
        String str2 = "%2s";
        sb2.append(String.format(str2, objArr).replace(' ', '0'));
        sb2.append(String.format(str2, new Object[]{Integer.valueOf(i10)}).replace(' ', '0'));
        sb2.append(String.format(str2, new Object[]{Integer.valueOf(i8)}).replace(' ', '0'));
        sb.append(sb2.toString());
        int i11 = parseInt % 86400;
        int i12 = i11 / 3600;
        int i13 = i11 % 3600;
        int i14 = i13 / 60;
        int i15 = i13 % 60;
        sb.append(String.format(str2, new Object[]{Integer.valueOf(i12)}).replace(' ', '0'));
        sb.append(String.format(str2, new Object[]{Integer.valueOf(i14)}).replace(' ', '0'));
        sb.append(String.format(str2, new Object[]{Integer.valueOf(i15)}).replace(' ', '0'));
        return sb.toString();
    }

    public static String m2959a(byte[] bArr, int i) {
        String str = "";
        for (int i2 = 0; i2 < i; i2++) {
            str = str.concat(String.format("%02X", new Object[]{Byte.valueOf(bArr[i2])}));
        }
        return str;
    }



}
