package org.cytoscape.aMatReader.internal.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MatrixParser {
	private Vector<String> rowNames;
	private Vector<String> columnNames;
	private final HashMap<Integer, Map<Integer, Double>> edgeMap;
	private final MatrixParameters parameters;

	public class MatrixParseException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2747259808393035815L;

		public MatrixParseException(String message) {
			super(message);
		}
	}

	public MatrixParser(final ResettableBufferedReader reader, MatrixParameters params)
			throws IOException, MatrixParseException {
		this.parameters = params;
		this.rowNames = new Vector<String>();
		this.columnNames = new PrefixedVector();
		edgeMap = new HashMap<Integer, Map<Integer, Double>>();
		buildNetwork(reader);
	}

	public int edgeCount() {
		int count = 0;
		for (int src : edgeMap.keySet()) {
			Map<Integer, Double> map = edgeMap.get(src);
			count += map.size();
		}
		return count;
	}

	public String getRowName(int i) {
		if (rowNames.isEmpty())
			return "Node " + i;
		return rowNames.get(i);
	}

	public int getRowCount() {
		return rowNames.size();
	}

	public int getColumnCount() {
		return columnNames.size();
	}

	public String getColumnName(int i) {
		if (columnNames.isEmpty())
			return "Node " + i;
		return columnNames.get(i);
	}

	public Map<Integer, Map<Integer, Double>> getEdges() {
		return edgeMap;
	}

	private void readColumnNames(String[] names) {
		for (int start = parameters.hasRowNames ? 1 : 0; start < names.length; start++) {
			if (!names[start].isEmpty()) {
				String name = names[start];
				if (name.startsWith(parameters.columnPrefix)) {
					name = name.substring(parameters.columnPrefix.length(), name.length());
				}
				columnNames.add(name);
			}
		}
	}

	private void parseRow(int rowNumber, String[] row, boolean undirected) {
		int start = undirected ? rowNumber + 1 : 0;
		HashMap<Integer, Double> tgtMap = new HashMap<Integer, Double>();
		for (; start < row.length; start++) {
			Double value = getValue(row[start]);
			if (value != null && !(parameters.ignoreZeros && value == 0)) {
				tgtMap.put(start, value);
			}
		}
		edgeMap.put(rowNumber, tgtMap);
	}

	private boolean buildNetwork(final ResettableBufferedReader reader) throws IOException, MatrixParseException {
		String[] row;
		int rowNumber = 0;
		boolean pastColumnLine = !parameters.hasColumnNames;
		while ((row = readRow(reader, parameters.delimiter)) != null) {

			// comment line or empty line
			if (row.length == 0) {
				continue;
			}
			// column names line
			if (!pastColumnLine) {
				readColumnNames(row);
				if (!parameters.hasRowNames) {
					rowNames = columnNames;
				}
				pastColumnLine = true;
				continue;
			}
			String name = "";
			if (parameters.hasRowNames) {
				name = row[0];
				rowNames.add(name);
				row = Arrays.copyOfRange(row, 1, row.length);
				if (!parameters.hasColumnNames) {
					if (columnNames.isEmpty()) {
						for (int i = 1; i <= row.length; i++) {
							columnNames.add("Node " + i);
						}
					}
					columnNames.set(rowNumber, name);
				}
			}

			if (name.isEmpty()) {
				name = getColumnName(rowNumber);
			}

			parseRow(rowNumber, row, parameters.undirected);
			rowNumber++;
		}
		return true;
	}

	private String[] readRow(ResettableBufferedReader input, Delimiter delimiter) throws IOException {
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

	private static Double getValue(String value) {
		Double v = null;
		if (value == null)
			return null;
		try {
			v = new Double(value);
			if (v.isNaN()) {
				v = null;
			}
		} catch (NumberFormatException nfe) {

		}
		return v;
	}

	public static class MatrixParameters {
		public MatrixParameters(Delimiter delimiter, boolean ignoreZeros, boolean hasRowNames, boolean hasColumnNames,
				boolean undirected) {
			this.delimiter = delimiter;
			this.hasRowNames = hasRowNames;
			this.hasColumnNames = hasColumnNames;
			this.ignoreZeros = ignoreZeros;
			this.undirected = undirected;
		}

		public MatrixParameters() {
		};

		public Delimiter delimiter = Delimiter.TAB;
		public boolean hasRowNames = false, hasColumnNames = false;
		public String columnPrefix = "";
		public boolean ignoreZeros = true;
		public boolean undirected = false;
	}

	public static MatrixParameters predictParameters(ResettableBufferedReader reader) throws IOException {
		MatrixParameters prediction = new MatrixParameters();
		try {
			String firstLine = "#";

			while (firstLine.startsWith("#")) {
				reader.peekLine();
				firstLine = reader.readLine();
			}
			String secondLine = "#";
			while (secondLine.startsWith("#")) {
				reader.peekLine();
				secondLine = reader.readLine();
			}

			int numItems = 0;
			for (Delimiter delim : Delimiter.values()) {
				String[] secondRow = secondLine.split(delim.delimiter, -1);

				if (secondRow.length > numItems) {
					numItems = secondRow.length;
					prediction.delimiter = delim;
				}
			}

			String[] firstRow = firstLine.split(prediction.delimiter.delimiter, -1);
			String[] secondRow = secondLine.split(prediction.delimiter.delimiter, -1);

			for (int i = 1; i < firstRow.length; i++) {
				String col = firstRow[i];
				if (!col.isEmpty() && getValue(col) == null) {
					prediction.hasColumnNames = true;
					break;
				}
			}
			if (!secondRow[0].isEmpty() && getValue(secondRow[0]) == null)
				prediction.hasRowNames = true;

			if (prediction.hasRowNames)
				firstRow = Arrays.copyOfRange(firstRow, 1, firstRow.length);

			PrefixedVector pv = new PrefixedVector(firstRow);

			if (pv.hasPrefix())
				prediction.columnPrefix = pv.getPrefix();

			if (prediction.hasColumnNames && prediction.hasRowNames) {
				if (secondRow[0].startsWith(prediction.columnPrefix)) {
					prediction.columnPrefix = "";
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		reader.reset();
		return prediction;
	}

	private Vector<String> getEdgeStrings() {
		Vector<String> edges = new Vector<String>();
		for (int s : edgeMap.keySet()) {
			Map<Integer, Double> tgtMap = edgeMap.get(s);
			for (int t : tgtMap.keySet()) {
				double value = tgtMap.get(t);
				edges.add(getRowName(s) + " - " + getColumnName(t) + " = " + value);
			}
		}
		return edges;
	}

	public void removeColumnPrefix() {
		if (parameters.columnPrefix != null && !parameters.columnPrefix.isEmpty())
			for (int i = 0; i < columnNames.size(); i++) {
				String name = columnNames.get(i);
				if (name.startsWith(parameters.columnPrefix)) {
					name = name.substring(parameters.columnPrefix.length());
					columnNames.set(i, name);
				}
			}
	}

	public static void main(String[] args) throws Exception {

		String[] files = new String[] { "/Users/bsettle/git/aMatReader/python/samples/sample.mat",
				"/Users/bsettle/git/aMatReader/python/samples/sampleNoHeaderRow.mat",
				"/Users/bsettle/git/aMatReader/python/samples/sampleNoHeaderColumn.mat",
				"/Users/bsettle/git/aMatReader/python/samples/crazyTest.csv",
				"/Users/bsettle/git/aMatReader/python/samples/crazyTest2.txt",
				"/Users/bsettle/git/aMatReader/python/samples/crazyTest3.txt" };
		String outputAnswer = null;
		for (String f : files) {
			System.out.println(f);
			FileInputStream in = new FileInputStream(f);
			ResettableBufferedReader reader = new ResettableBufferedReader(new BufferedInputStream(in));
			MatrixParameters pred = MatrixParser.predictParameters(reader);
			pred.ignoreZeros = true;
			pred.undirected = false;
			MatrixParser p = new MatrixParser(reader, pred);
			Vector<String> strs = p.getEdgeStrings();
			String[] strsArr = new String[strs.size()];
			strs.toArray(strsArr);
			Arrays.sort(strsArr);

			String output = String.join("\n", strsArr);

			if (outputAnswer == null) {
				System.out.println(" = \n" + output);
				outputAnswer = output;
			} else {
				if (!output.equals(outputAnswer)) {
					System.out.println(" mismatch = \n" + output);
					break;
				} else {
					System.out.println("match");
				}
			}
		}
	}

}
