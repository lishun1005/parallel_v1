package com.rsclouds.gtparallel.gtdata.utills;

import java.io.CharArrayWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.BitSet;

/**
 * gt-data路径编码转码工具类
 * @author root
 *
 */
public class TransCoding {
	static BitSet dontNeedEncoding;
	static final int caseDiff = ('a' - 'A');
	static String dfltEncName = null;
	static {
		dontNeedEncoding = new BitSet(256);
		int i;
		for (i = 'a'; i <= 'z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = 'A'; i <= 'Z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = '0'; i <= '9'; i++) {
			dontNeedEncoding.set(i);
		}
		dontNeedEncoding.set('-');
		dontNeedEncoding.set('_');
		dontNeedEncoding.set('.');
		dontNeedEncoding.set('*');
		dontNeedEncoding.set('.');
		dontNeedEncoding.set('~');
		dontNeedEncoding.set('$');
		dontNeedEncoding.set('&');
		dontNeedEncoding.set('#');
		dontNeedEncoding.set('+');
		dontNeedEncoding.set(',');
		dontNeedEncoding.set('/');
		dontNeedEncoding.set(':');
		dontNeedEncoding.set(';');
		dontNeedEncoding.set('=');
		dontNeedEncoding.set('?');
		dontNeedEncoding.set('@');
		dontNeedEncoding.set('!');
		dontNeedEncoding.set('\\');
		dontNeedEncoding.set('(');
		dontNeedEncoding.set(')');
	}

	public static  String UrlEncode(String s, String enc){

		boolean needToChange = false;
		StringBuffer out = new StringBuffer(s.length());
		Charset charset;
		CharArrayWriter charArrayWriter = new CharArrayWriter();

		if (enc == null)
			throw new NullPointerException("charsetName");

		try {
			charset = Charset.forName(enc);
		}catch (Exception e) {
			charset = Charset.forName("utf-8");
		}

		for (int i = 0; i < s.length();) {
			int c = (int) s.charAt(i);
			// System.out.println("Examining character: " + c);
			if (dontNeedEncoding.get(c)) {
				out.append((char) c);
				i++;
			} else {
				// convert to external encoding before hex conversion
				do {
					charArrayWriter.write(c);
					/*
					 * If this character represents the start of a Unicode
					 * surrogate pair, then pass in two characters. It's not
					 * clear what should be done if a bytes reserved in the
					 * surrogate pairs range occurs outside of a legal surrogate
					 * pair. For now, just treat it as if it were any other
					 * character.
					 */
					if (c >= 0xD800 && c <= 0xDBFF) {
						/*
						 * System.out.println(Integer.toHexString(c) +
						 * " is high surrogate");
						 */
						if ((i + 1) < s.length()) {
							int d = (int) s.charAt(i + 1);
							/*
							 * System.out.println("\tExamining " +
							 * Integer.toHexString(d));
							 */
							if (d >= 0xDC00 && d <= 0xDFFF) {
								/*
								 * System.out.println("\t" +
								 * Integer.toHexString(d) +
								 * " is low surrogate");
								 */
								charArrayWriter.write(d);
								i++;
							}
						}
					}
					i++;
				} while (i < s.length()
						&& !dontNeedEncoding.get((c = (int) s.charAt(i))));

				charArrayWriter.flush();
				String str = new String(charArrayWriter.toCharArray());
				byte[] ba = str.getBytes(charset);
				for (int j = 0; j < ba.length; j++) {
					out.append('%');
					char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
					// converting to use uppercase letter as part of
					// the hex value if ch is a letter.
					if (Character.isLetter(ch)) {
						ch -= caseDiff;
					}
					out.append(ch);
					ch = Character.forDigit(ba[j] & 0xF, 16);
					if (Character.isLetter(ch)) {
						ch -= caseDiff;
					}
					out.append(ch);
				}
				charArrayWriter.reset();
				needToChange = true;
			}
		}
		return (needToChange ? out.toString() : s);
	}

	
	 /**
     * Decodes a <code>application/x-www-form-urlencoded</code> string using a specific
     * encoding scheme.
     * The supplied encoding is used to determine
     * what characters are represented by any consecutive sequences of the
     * form "<code>%<i>xy</i></code>".
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilites.</em>
     *
     * @param s the <code>String</code> to decode
     * @param enc   The name of a supported
     *    <a href="../lang/package-summary.html#charenc">character
     *    encoding</a>.
     * @return the newly decoded <code>String</code>
     * @exception  UnsupportedEncodingException
     *             If character encoding needs to be consulted, but
     *             named character encoding is not supported
     * @see URLEncoder#encode(java.lang.String, java.lang.String)
     * @since 1.4
     */
    public static String decode(String s, String enc){

        boolean needToChange = false;
        int numChars = s.length();
        StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;

        if (enc == null || enc.length() == 0) {
//            throw new UnsupportedEncodingException ("URLDecoder: empty string enc parameter");
        	enc = "utf-8";
        }

        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
            case '+':
                sb.append(' ');
                i++;
                needToChange = true;
                break;
            case '%':
                /*
                 * Starting with this instance of %, process all
                 * consecutive substrings of the form %xy. Each
                 * substring %xy will yield a byte. Convert all
                 * consecutive  bytes obtained this way to whatever
                 * character(s) they represent in the provided
                 * encoding.
                 */

                try {

                    // (numChars-i)/3 is an upper bound for the number
                    // of remaining bytes
                    if (bytes == null)
                        bytes = new byte[(numChars-i)/3];
                    int pos = 0;

                    while ( ((i+2) < numChars) &&
                            (c=='%')) {
                        int v = Integer.parseInt(s.substring(i+1,i+3),16);
                        if (v < 0)
                            throw new IllegalArgumentException("URLDecoder: Illegal hex characters in escape (%) pattern - negative value");
                        bytes[pos++] = (byte) v;
                        i+= 3;
                        if (i < numChars)
                            c = s.charAt(i);
                    }

                    // A trailing, incomplete byte encoding such as
                    // "%x" will cause an exception to be thrown

                    if ((i < numChars) && (c=='%'))
                        throw new IllegalArgumentException(
                         "URLDecoder: Incomplete trailing escape (%) pattern");

                    sb.append(new String(bytes, 0, pos, enc));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                    "URLDecoder: Illegal hex characters in escape (%) pattern - "
                    + e.getMessage());
                } catch (UnsupportedEncodingException e) { 
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                needToChange = true;
                break;
            default:
                sb.append(c);
                i++;
                break;
            }
        }

        return (needToChange? sb.toString() : s);
    }
	public static void main(String args[]) throws UnsupportedEncodingException {
		// new TransCoding().UrlEncode("F182012.v4c.avg_lights_x_pct.tar",
		// "utf-8");
		// System.out.println(java.net.URLEncoder.encode("F182012.v4c.avg_lights_x_pct.tar",
		// "utf-8"));
		TransCoding coding = new TransCoding();
		String encodeStr = coding.UrlEncode("landsat8(��������)", "utf-8");
		System.out.println(encodeStr);
		//System.out.println(java.net.URLDecoder.decode(
		//		"resource/XTYY/tilesBuckup/%E4%BA%91%E5%B9%B3%E5%8F%B0/%E4%BA%91%E5%B9%B3%E5%8F%B0%E4%B8%93%E9%A2%98/%E4%BA%AC%E6%B4%A5%E5%86%80%E4%B8%93%E9%A2%98/%E4%BA%AC%E6%B4%A5%E5%86%80_%E6%A4%8D%E8%A2%AB%E6%8C%87%E6%95%B0%E5%90%88%E6%88%90_20140101-20140110/Layers/_alllayers//", "utf-8"));

	}
}
