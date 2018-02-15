package org.cytoscape.aMatReader.internal.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MatrixParser {
	private Vector<String> rowNames;
	private PrefixedVector columnNames;
	private final HashMap<Integer, Map<Integer, Double>> edgeMap;
	private final boolean ignoreZeros;

	public MatrixParser(final ResettableBufferedReader reader, final Delimiter delim, final boolean ignoreZeros,
			final boolean hasRowNames, boolean hasColumnNames, MatrixSymmetry matrixTriangles) {
		this.ignoreZeros = ignoreZeros;
		this.rowNames = new Vector<String>();
		this.columnNames = new PrefixedVector();
		edgeMap = new HashMap<Integer, Map<Integer, Double>>();
		importFile(reader, delim, hasRowNames, hasColumnNames, matrixTriangles);
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

	public void readColumnNames(String[] names, int start) {
		for (; start < names.length; start++) {
			if (!names[start].isEmpty())
				columnNames.add(names[start]);
		}
	}

	public Map<Integer, Double> parseRow(String[] row, int startIndex, int endIndex) {
		Map<Integer, Double> tgtMap = new HashMap<Integer, Double>();
		for (; startIndex < row.length && startIndex < endIndex; startIndex++) {
			Double value = getValue(row[startIndex]);
			if (value != null && !(ignoreZeros && value == 0)) {
				tgtMap.put(startIndex, value);
			}
		}
		return tgtMap;
	}

	public void importFile(final ResettableBufferedReader reader, Delimiter delim, final boolean hasRowNames,
			final boolean hasColumnNames, MatrixSymmetry matrixTriangles) {
		try {
			String[] row;
			int rowNumber = 0;
			boolean pastColumnNames = !hasColumnNames;
			while ((row = readRow(reader, delim)) != null) {
				if (row.length == 0)
					continue;
				if (rowNumber == 0 && !pastColumnNames) {
					readColumnNames(row, hasRowNames ? 1 : 0);
					pastColumnNames = true;
					continue;
				} else {
					int start = matrixTriangles == MatrixSymmetry.SYMMETRIC_TOP ? rowNumber + 1 : 0;
					int end = matrixTriangles == MatrixSymmetry.SYMMETRIC_BOTTOM ? rowNumber + 1 : row.length;

					if (hasRowNames) {
						rowNames.add(row[0]);
						row = Arrays.copyOfRange(row, 1, row.length);
					}
					Map<Integer, Double> tgtMap = parseRow(row, start, end);

					edgeMap.put(rowNumber, tgtMap);
				}
				rowNumber++;
			}

			if (hasRowNames && !hasColumnNames)
				columnNames = new PrefixedVector(rowNames);
			else if (hasColumnNames && !hasRowNames)
				rowNames = columnNames;

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

	static Double getValue(String value) {
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

	public static class MatrixParameterPrediction {
		public Delimiter delimiter = Delimiter.TAB;
		public boolean hasRowNames = false, hasColumnNames = false;
		public String columnPrefix = "";
	}

	public static MatrixParameterPrediction predictParameters(ResettableBufferedReader reader) throws IOException {
		MatrixParameterPrediction prediction = new MatrixParameterPrediction();
		try {
			String firstLine = "#";
			while (firstLine.startsWith("#")) {
				firstLine = reader.readLine();
			}
			String secondLine = "#";
			while (secondLine.startsWith("#")) {
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

			if (prediction.hasRowNames && secondRow[0].startsWith(prediction.columnPrefix)) {
				prediction.columnPrefix = "";
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		reader.reset();

		return prediction;
	}

	public void removeColumnPrefix() {
		if (columnNames.hasPrefix())
			for (int i = 0; i < columnNames.size(); i++) {
				columnNames.set(i, columnNames.get(i).substring(columnNames.getPrefix().length()));
			}
	}

}
