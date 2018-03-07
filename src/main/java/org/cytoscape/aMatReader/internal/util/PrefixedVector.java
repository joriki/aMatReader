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
	
	private void updatePrefix(String s){
		int lastIndex = s.lastIndexOf('.');
		if (lastIndex >= 0){
			if (size() == 0){
				prefix = s.substring(0, lastIndex + 1);
			}else{
				if (s.startsWith(prefix))
					return;
				else{
					while (lastIndex > 0){
						lastIndex = s.lastIndexOf('.', lastIndex);
						prefix = s.substring(0, lastIndex + 1);
						if (s.startsWith(prefix))
							break;
					}
				}
			}
		}else{
			prefix = "";
		}
	}

	public boolean add(String s) {
		if (s.isEmpty())
			return false;
		updatePrefix(s);
		return super.add(s);
	}

	public boolean hasPrefix() {
		return !prefix.isEmpty();
	}

	public String getPrefix() {
		return prefix;
	}
}
