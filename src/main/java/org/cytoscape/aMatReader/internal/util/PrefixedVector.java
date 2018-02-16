package org.cytoscape.aMatReader.internal.util;

import java.util.Vector;

public class PrefixedVector extends Vector<String> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7863329734788756862L;
	private String prefix = "";

	public PrefixedVector() {
		super();
	}

	public PrefixedVector(Vector<String> v) {
		super();
		for (String s : v) {
			add(s);
		}
	}

	public PrefixedVector(String[] v) {
		super();
		for (String s : v) {
			add(s);
		}
	}

	public boolean add(String s) {
		if (s.isEmpty())
			return false;
		if (size() == 0)
			prefix = s;
		else {
			int i = 0;
			for (; i < s.length() && i < prefix.length() && prefix.charAt(i) == s.charAt(i); i++)
				;
			prefix = prefix.substring(0, i);
		}
		return super.add(s);
	}

	public boolean hasPrefix() {
		return !prefix.isEmpty();
	}

	public String getPrefix() {
		return prefix;
	}
}
