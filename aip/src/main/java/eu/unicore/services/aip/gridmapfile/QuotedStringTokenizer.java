package eu.unicore.services.aip.gridmapfile;

import java.util.Enumeration;

public class QuotedStringTokenizer implements Enumeration<String> {

	private int limit;
	private int start;
	private final String str;

	public QuotedStringTokenizer(String str) {
		this.str = str;
		start = 0;
		limit = str.length();
	}

	@Override
	public String nextElement() {
		while ((start < limit) && Character.isWhitespace(str.charAt(start))) {
			start++;	// eliminate leading whitespace
		}
		if (start == limit) return null;
		StringBuffer buf = new StringBuffer(limit-start);
		char ch;
		char quote = str.charAt(start);
		if (quote == '"' || quote == '\'') {
			start++;
			for (int i=start;i<limit;i++) {
				ch = str.charAt(i);
				start++;
				if (ch == quote) {
					break;
				} else if (ch == '\\') {
					buf.append( str.charAt(++i) );
					start++;
				} else {
					buf.append(ch);
				}
			}
			return buf.toString();
		} else {
			for (int i=start;i<limit;i++) {
				ch = str.charAt(i);
				start++;
				if (Character.isWhitespace(ch)) {
					break;
				} else {
					buf.append(ch);
				}
			}
		}
		return buf.toString();
	}

	@Override
	public boolean hasMoreElements() {
		return hasMoreTokens();
	}

	public boolean hasMoreTokens() {
		while ((start < limit) && (str.charAt(start) <= ' ')) {
			start++;	// eliminate leading whitespace
		}

		return (start != limit);
	}

}