
package net.jforum.util.bbcode;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.jforum.exceptions.ForumException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("serial")
public class BBCodeHandler extends DefaultHandler implements Serializable {

	@SuppressWarnings("rawtypes")
	private Map bbMap            = new LinkedHashMap();
	@SuppressWarnings("rawtypes")
	private Map alwaysProcessMap = new LinkedHashMap();

	private String tagName  = "";
	private StringBuffer sb = null;
	private BBCode bb       = null;

	public BBCodeHandler() {

	}

	public BBCodeHandler parse() {
		try {
			System.out.println("--> [BBCodeHandler.parse] to generate this class instance ......");
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			BBCodeHandler bbParser = new BBCodeHandler();
			String path = SystemGlobals.getValue(ConfigKeys.CONFIG_DIR) + "/bb_config.xml";
			File fileInput = new File(path);
			if (fileInput.exists()) {
				parser.parse(fileInput, bbParser);
			} else {
				InputSource input = new InputSource(path);
				parser.parse(input, bbParser);
			}
			return bbParser;
		} catch (Exception e) {
			throw new ForumException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void addBb(BBCode bb) {
		if (bb.alwaysProcess()) {
			alwaysProcessMap.put(bb.getTagName(), bb);
		} else {
			bbMap.put(bb.getTagName(), bb);
		}
	}

	@SuppressWarnings("rawtypes")
	public Collection getBbList() {
		return bbMap.values();
	}

	@SuppressWarnings("rawtypes")
	public Collection getAlwaysProcessList() {
		return alwaysProcessMap.values();
	}

	public BBCode findByName(String tagName) {
		return (BBCode)bbMap.get(tagName);
	}

	public void startElement(String uri, String localName, String tag, Attributes attrs) {
		if (tag.equals("match")) {
			sb = new StringBuffer();
			bb = new BBCode();
			String tagName = attrs.getValue("name");
			if (tagName != null) {
				bb.setTagName(tagName);
			}
			// Shall we remove the infamous quotes?
			String removeQuotes = attrs.getValue("removeQuotes");
			if (removeQuotes != null && removeQuotes.equals("true")) {
				bb.enableRemoveQuotes();
			}
			String alwaysProcess = attrs.getValue("alwaysProcess");
			if (alwaysProcess != null && "true".equals(alwaysProcess)) {
				bb.enableAlwaysProcess();
			}
		}
		tagName = tag;
	}

	public void endElement(String uri, String localName, String tag) {
		if (tag.equals("match")) {
			addBb(bb);
		} else if (tagName.equals("replace")) {
			bb.setReplace(sb.toString().trim());
			sb.delete(0, sb.length());
		} else if (tagName.equals("regex")) {
			bb.setRegex(sb.toString().trim());
			sb.delete(0, sb.length());
		}
		tagName = "";
	}

	public void characters(char ch[], int start, int length) {
		if (tagName.equals("replace") || tagName.equals("regex")) {
			sb.append(ch, start, length);
		}
	}

	public void error(SAXParseException exception) throws SAXException {
		throw exception;
	}
}