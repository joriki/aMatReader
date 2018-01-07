package org.cytoscape.aMatReader.internal.tasks;

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

import org.cytoscape.aMatReader.internal.rest.ColumnHeadersFormat;
import org.cytoscape.aMatReader.internal.rest.Delimiter;
import org.cytoscape.aMatReader.internal.rest.RowHeadersFormat;

public class MatrixParser {
	private final Vector<String> sourceNames, targetNames;
	private final HashMap<Integer, Map<Integer, Double>> edgeMap;

	public MatrixParser(InputStream is, Delimiter delim, boolean undirected, RowHeadersFormat rowHeaders,
			ColumnHeadersFormat columnHeaders) {
		
		sourceNames = new Vector<String>();
		targetNames = new Vector<String>();
		edgeMap = new HashMap<Integer, Map<Integer, Double>>();

		importFile(is, delim, undirected, columnHeaders, rowHeaders);
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
	
	public int sourceCount(){
		return sourceNames.size();
	}
	
	public int targetCount(){
		return targetNames.size();
	}

	public String getTargetName(int i) {
		return targetNames.get(i);
	}

	public Map<Integer, Map<Integer, Double>> getEdges() {
		return edgeMap;
	}

	
	public void readColumnHeaders(String[] names) {
		for (String name : names) {
			if (name.length() > 0)
				targetNames.add(name);
		}
	}

	public Map<Integer, Double> parseRow(String[] row, int d) {
		Map<Integer, Double> tgtMap = new HashMap<Integer, Double>();
		for (int index = 0; index + d < row.length; index++) {
			Double value = getValue(row[index + d]);
			if (value != null)
				tgtMap.put(index, value);
		}
		return tgtMap;
	}

	public void importFile(InputStream is, Delimiter delim, boolean undirected, ColumnHeadersFormat columnHeaders,
			RowHeadersFormat rowHeaders) {

		BufferedReader input = new BufferedReader(new InputStreamReader(is));
		try {
			String[] row;
			int rowNumber = 0;
			boolean header = columnHeaders == ColumnHeadersFormat.NONE;
			while ((row = readRow(input, delim)) != null) {
				if (row.length == 0)
					continue;
				if (rowNumber == 0 && !header) {
					if (columnHeaders == ColumnHeadersFormat.NAMES) {
						readColumnHeaders(row);
					}
					header = true;
					continue;
				} else {
					String name = "Node" + rowNumber;
					if (rowHeaders == RowHeadersFormat.NAMES)
						name = row[0];
					else if (columnHeaders == ColumnHeadersFormat.NAMES)
						name = targetNames.get(rowNumber);
					sourceNames.add(name);
					int start = rowHeaders == RowHeadersFormat.NONE ? 0 : 1;
					if (undirected){
						start += rowNumber;
					}
					//Map<Integer, Double> tgtMap = parseRow(row, rowHeaders);
					Map<Integer, Double> tgtMap = parseRow(row, start);

					if (columnHeaders != ColumnHeadersFormat.NAMES)
						targetNames.add(name);
					//System.out.printf("%d -> %d edges\n", rowNumber, tgtMap.size());
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

	public static void main(String[] args) throws FileNotFoundException {
		InputStream inputStream = new FileInputStream("/Users/bsettle/Desktop/adjs/qval_orig.txt");
		MatrixParser parser = new MatrixParser(inputStream, Delimiter.TAB, true, RowHeadersFormat.NAMES,
				ColumnHeadersFormat.IGNORE);

		System.out.println(parser.sourceNames.size() + " sources");
		System.out.println(parser.targetNames.size() + " targets");
		
		
		for (int src : parser.edgeMap.keySet()) {
			Map<Integer, Double> map = parser.edgeMap.get(src);
			// System.out.println(src);
			String srcName = parser.sourceNames.get(src);
			for (int tgt : map.keySet()) {
				String tgtName = parser.targetNames.get(tgt);
				double value = map.get(tgt);
				System.out.printf("%s - %s: %f\n", srcName, tgtName, value);
			}
		}
		
	}

	public static Delimiter predictDelimiter(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line1 = reader.readLine();
		int max = 0;
		Delimiter delimiter = Delimiter.TAB;
		for (Delimiter delim : Delimiter.values()){
			int n = line1.split(delim.getDelimiter()).length;
			if (n > max){
				delimiter = delim;
				max = n;
			}
		}
		reader.close();
		return delimiter;
	}
}
