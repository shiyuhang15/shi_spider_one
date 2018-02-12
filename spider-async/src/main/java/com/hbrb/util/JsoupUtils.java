package com.hbrb.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.client.entity.DeflateInputStream;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.model.AsyncResult;
import com.hbrb.spider.model.page.HtmlPage;
import com.hbrb.spider.model.task.RequestTask;

public class JsoupUtils {
	public static <T extends RequestTask> HtmlPage<T> buildHtmlPage(AsyncResult<T> rawResult) throws IOException {
		ByteBuffer byteData;
		ByteBuffer rawData = rawResult.getRawData();
		switch (rawResult.getContentEncoding()) {
		case ConstantsHome.ENCODING_GZIP:
			ByteArrayInputStream bais = new ByteArrayInputStream(rawData.array());
			GZIPInputStream input = new GZIPInputStream(bais);
			byte[] byteArray = IOUtils.toByteArray(input);
			byteData = ByteBuffer.wrap(byteArray);
			break;
		case ConstantsHome.ENCODING_DEFLATE:
			ByteArrayInputStream bais2 = new ByteArrayInputStream(rawData.array());
			DeflateInputStream input2 = new DeflateInputStream(bais2);
			byte[] byteArray2 = IOUtils.toByteArray(input2);
			byteData = ByteBuffer.wrap(byteArray2);
			break;
		default:
			byteData = rawData;
			break;
		}
		String lastRedirectLocation = rawResult.getLastRedirectLocation();
		return new HtmlPage<T>(rawResult.getRequestTask(), parseByteData(byteData, rawResult.getCharsetName(),
				lastRedirectLocation == null ? rawResult.getRequestTask().getUrl() : lastRedirectLocation));
	}
	// 下面几个方法从 jsoup 1.10.3 org.jsoup.helper.DataUtil 里搬过来的
	public static Document parseByteData(ByteBuffer byteData, String charsetName, String baseUri) {
		Parser parser = Parser.htmlParser();
        String docData;
        Document doc = null;

        // look for BOM - overrides any other header or input
        charsetName = detectCharsetFromBom(byteData, charsetName);

        if (charsetName == null) { // determine from meta. safe first parse as UTF-8
            // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
            docData = Consts.UTF_8.decode(byteData).toString();
            doc = parser.parseInput(docData, baseUri);
            Elements metaElements = doc.select("meta[http-equiv=content-type], meta[charset]");
            String foundCharset = null; // if not found, will keep utf-8 as best attempt
            for (Element meta : metaElements) {
                if (meta.hasAttr("http-equiv")) {
                    foundCharset = getCharsetFromContentType(meta.attr("content"));
                }
                if (foundCharset == null && meta.hasAttr("charset")) {
                    foundCharset = meta.attr("charset");
                }
                if (foundCharset != null) {
                    break;
                }
            }

            // look for <?xml encoding='ISO-8859-1'?>
            if (foundCharset == null && doc.childNodeSize() > 0 && doc.childNode(0) instanceof XmlDeclaration) {
                XmlDeclaration prolog = (XmlDeclaration) doc.childNode(0);
                if (prolog.name().equals("xml")) {
                    foundCharset = prolog.attr("encoding");
                }
            }
            foundCharset = validateCharset(foundCharset);

            if (foundCharset != null && !foundCharset.equalsIgnoreCase(Consts.UTF_8.name())) { // need to re-decode
                foundCharset = foundCharset.trim().replaceAll("[\"']", "");
                charsetName = foundCharset;
                byteData.rewind();
                docData = Charset.forName(foundCharset).decode(byteData).toString();
                doc = null;
            }
        } else { // specified by content type header (or by user on file load)
            docData = Charset.forName(charsetName).decode(byteData).toString();
        }
        if (doc == null) {
            doc = parser.parseInput(docData, baseUri);
            doc.outputSettings().charset(charsetName);
        }
        return doc;
    }
    private static String detectCharsetFromBom(ByteBuffer byteData, String charsetName) {
        byteData.mark();
        byte[] bom = new byte[4];
        if (byteData.remaining() >= bom.length) {
            byteData.get(bom);
            byteData.rewind();
        }
        if (bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF || // BE
            bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == 0x00 && bom[3] == 0x00) { // LE
            charsetName = "UTF-32"; // and I hope it's on your system
        } else if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF || // BE
            bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
            charsetName = "UTF-16"; // in all Javas
        } else if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            charsetName = "UTF-8"; // in all Javas
            byteData.position(3); // 16 and 32 decoders consume the BOM to determine be/le; utf-8 should be consumed here
        }
        return charsetName;
    }
    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*(?:\"|')?([^\\s,;\"']*)");
    private static String getCharsetFromContentType(String contentType) {
        if (contentType == null) return null;
        Matcher m = charsetPattern.matcher(contentType);
        if (m.find()) {
            String charset = m.group(1).trim();
            charset = charset.replace("charset=", "");
            return validateCharset(charset);
        }
        return null;
    }
    private static String validateCharset(String cs) {
        if (cs == null || cs.length() == 0) return null;
        cs = cs.trim().replaceAll("[\"']", "");
        try {
            if (Charset.isSupported(cs)) return cs;
            cs = cs.toUpperCase(Locale.ENGLISH);
            if (Charset.isSupported(cs)) return cs;
        } catch (IllegalCharsetNameException e) {
            // if our this charset matching fails.... we just take the default
        }
        return null;
    }
}
