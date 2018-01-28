package org.cytoscape.aMatReader.internal.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MatrixParser2 {
	private final Vector<String> sourceNames, targetNames;
	private final HashMap<Integer, Map<Integer, Double>> edgeMap;
	private final boolean ignoreZeros, removeSourcePrefix, removeTargetPrefix;
	private String sourcePrefix = null, targetPrefix = null;

	public MatrixParser2(InputStream is, Delimiter delim, boolean undirected, boolean ignoreZeros,
			boolean removeSourcePrefix, boolean removeTargetPrefix) {
		this.ignoreZeros = ignoreZeros;
		sourceNames = new Vector<String>();
		targetNames = new Vector<String>();
		edgeMap = new HashMap<Integer, Map<Integer, Double>>();
		this.removeSourcePrefix = removeSourcePrefix;
		this.removeTargetPrefix = removeTargetPrefix;

		importFile(is, delim, undirected);
	}

	public int edgeCount() {
		int count = 0;
		for (int src : edgeMap.keySet()) {
			Map<Integer, Double> map = edgeMap.get(src);
			count += map.size();
		}
		return count;
	}

	public String getSourceName(int i) {
		String name = sourceNames.get(i);
		if (removeSourcePrefix)
			name = name.substring(sourcePrefix.length());
		return name;
	}

	public int sourceCount() {
		return sourceNames.size();
	}

	public int targetCount() {
		return targetNames.size();
	}

	public String getTargetName(int i) {
		String name = targetNames.get(i);
		if (removeTargetPrefix)
			name = name.substring(targetPrefix.length());
		return name;
	}

	public Map<Integer, Map<Integer, Double>> getEdges() {
		return edgeMap;
	}

	public void readColumnHeaders(String[] names, int start) {
		for (; start < names.length; start++) {
			String name = names[start];
			addNode(name, 1);
		}

	}

	public Map<Integer, Double> parseRow(String[] row, int start) {
		Map<Integer, Double> tgtMap = new HashMap<Integer, Double>();
		for (int index = 0; index + start < row.length; index++) {
			Double value = getValue(row[index + start]);
			if (value != null && !(ignoreZeros && value == 0)) {
				tgtMap.put(index + start - 1, value);
			}
		}
		return tgtMap;
	}

	private void addNode(String name, int axis) {
		if (name.isEmpty())
			return;

		boolean toRemove = axis == 0 ? removeSourcePrefix : removeTargetPrefix;
		if (toRemove) {
			String prefix = axis == 0 ? sourcePrefix : targetPrefix;
			if (prefix == null){
				if (axis == 0)
					sourcePrefix = name;
				else
					targetPrefix = name;
			}else if (!prefix.isEmpty()) {
				int j = 0;
				for (; j < prefix.length() && j < name.length(); j++) {
					if (prefix.charAt(j) != name.charAt(j))
						break;
				}
				if (axis == 0)
					sourcePrefix = prefix.substring(0, j);
				else
					targetPrefix = prefix.substring(0, j);
			}
		}
		
		if (axis == 0)
			sourceNames.add(name);
		else
			targetNames.add(name);
	}

	public void importFile(InputStream is, Delimiter delim, boolean undirected) {

		BufferedReader input = new BufferedReader(new InputStreamReader(is));
		try {
			String[] row;
			int rowNumber = 0;
			while ((row = readRow(input, delim)) != null) {
				if (row.length == 0)
					continue;
				if (rowNumber == 0) {
					readColumnHeaders(row, 1);
				} else {
					String name = row[0];
					
					addNode(name, 0);
					
					int start = 1;
					if (undirected) {
						start += rowNumber;
					}
					Map<Integer, Double> tgtMap = parseRow(row, start);
					edgeMap.put(rowNumber, tgtMap);
				}
				rowNumber++;
			}

		} catch (IOException ioe) {
			System.out.println("IOE");
		}

	}

	String[] readRow(BufferedReader input, Delimiter delimiter) throws IOException {
		String row = input.readLine();
		if (row == null)
			return null;
		if (row.startsWith("#"))
			return new String[0];

		String[] columns;
		String delim = delimiter.getDelimiter();
		columns = row.split(delim, -1);

		return columns;
	}

	Double getValue(String value) {
		Double v = null;
		try {
			v = new Double(value);
			if (v.isNaN()) {
				v = null;
			}
		} catch (NumberFormatException nfe) {

		}

		return v;
	}

	public static Delimiter predictDelimiter(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line1 = reader.readLine();
		while (line1.startsWith("#"))
			line1 = reader.readLine();
		int max = 0;
		Delimiter delimiter = Delimiter.TAB;
		for (Delimiter delim : Delimiter.values()) {
			int n = line1.split(delim.getDelimiter(), -1).length;
			if (n > max) {
				delimiter = delim;
				max = n;
			}
		}
		reader.close();
		return delimiter;
	}
}
