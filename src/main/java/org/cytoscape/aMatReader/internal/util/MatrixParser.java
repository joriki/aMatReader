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

public class MatrixParser {
	private final Vector<String> sourceNames, targetNames;
	private final HashMap<Integer, Map<Integer, Double>> edgeMap;
	private final boolean ignoreZeros;

	public MatrixParser(InputStream is, Delimiter delim, boolean undirected, boolean ignoreZeros,
			RowNameState headerColumn, ColumnNameState headerRow) {
		this.ignoreZeros = ignoreZeros;
		sourceNames = new Vector<String>();
		targetNames = new Vector<String>();
		edgeMap = new HashMap<Integer, Map<Integer, Double>>();
		importFile(is, delim, undirected, headerColumn, headerRow);
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
		return sourceNames.get(i);
	}

	public int sourceCount() {
		return sourceNames.size();
	}

	public int targetCount() {
		return targetNames.size();
	}

	public String getTargetName(int i) {
		return targetNames.get(i);
	}

	public Map<Integer, Map<Integer, Double>> getEdges() {
		return edgeMap;
	}

	public void readColumnHeaders(String[] names, int start) {
		for (; start < names.length; start++) {
			if (names[start].length() > 0)
				targetNames.add(names[start]);
		}
	}

	public Map<Integer, Double> parseRow(String[] row, int rowNumber, boolean undirected, RowNameState headerColumn) {
		Map<Integer, Double> tgtMap = new HashMap<Integer, Double>();
		
		int dHeader = (headerColumn == RowNameState.NONE ? 0 : 1);
		
		int start = undirected ? rowNumber : 0;
		for (int index = 0; index + start + dHeader < row.length; index++) {
			int true_index = index + start;
			Double value = getValue(row[true_index + dHeader]);
			if (value != null && !(ignoreZeros && value == 0)) {
				tgtMap.put(true_index, value);
			}
		}
		return tgtMap;
	}

	public void importFile(InputStream is, Delimiter delim, boolean undirected, RowNameState headerColumn,
			ColumnNameState headerRow) {

		BufferedReader input = new BufferedReader(new InputStreamReader(is));
		try {
			String[] row;
			int rowNumber = 0;
			boolean pastHeader = headerRow == ColumnNameState.NONE;
			while ((row = readRow(input, delim)) != null) {
				if (row.length == 0)
					continue;
				if (rowNumber == 0 && !pastHeader) {
					if (headerRow == ColumnNameState.NAMES) {
						readColumnHeaders(row, headerColumn == RowNameState.NAMES ? 1 : 0);
					}
					pastHeader = true;
					continue;
				} else {
					String name = "Node" + (rowNumber + 1);

					if (headerColumn == RowNameState.NAMES)
						name = row[0];
					else if (headerRow == ColumnNameState.NAMES)
						name = targetNames.get(rowNumber);

					sourceNames.add(name);
					
					Map<Integer, Double> tgtMap = parseRow(row, rowNumber, undirected, headerColumn);

					if (headerRow != ColumnNameState.NAMES)
						targetNames.add(name);

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

	public static void main(String[] args) throws FileNotFoundException {
		File f = new File("samples/sampleNoHeaderRow.mat");
		System.out.println(f.getAbsolutePath());
		InputStream is = new FileInputStream(f);
		
		MatrixParser parser = new MatrixParser(is, Delimiter.TAB, true, false, RowNameState.IGNORE,
				ColumnNameState.NONE);
		System.out.println("Sources: " + String.join(", ", parser.sourceNames));
		System.out.println("Targets: " + String.join(", ", parser.targetNames));
		System.out.println("Edges: " + parser.edgeCount());

		for (int i : parser.edgeMap.keySet()) {
			String src = parser.getSourceName(i);
			for (int j : parser.edgeMap.get(i).keySet()) {
				String tgt = parser.getTargetName(j);
				Double v = parser.edgeMap.get(i).get(j);
				if (v != null)
					System.out.println(i + ":" + src + " " + j + ":" + tgt + " " + v);
			}
		}

	}
}
