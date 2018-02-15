package org.cytoscape.aMatReader.internal.util;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import javax.swing.*;

public class MatrixTriangleSelector extends JPanel implements ComponentListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ToggleTriangleAreaButton topRightButton, bottomLeftButton;
	private ToggleAreaButton rowNamesButton, columnNamesButton;
	public MatrixTriangleSelector(boolean rowNames, boolean columnNames, MatrixSymmetry triangle) {
		super();
		setLayout(null);
		int width = 200, height = 200;
		setSize(width, height);

		topRightButton = new ToggleTriangleAreaButton("Top right", true);
		topRightButton.setSelected(triangle != MatrixSymmetry.SYMMETRIC_BOTTOM);
		bottomLeftButton = new ToggleTriangleAreaButton("Bottom left", false);
		bottomLeftButton.setSelected(triangle != MatrixSymmetry.SYMMETRIC_TOP);
		rowNamesButton = new ToggleAreaButton("Row Names");
		rowNamesButton.setSelected(rowNames);
		columnNamesButton = new ToggleAreaButton("Column Names");
		columnNamesButton.setSelected(columnNames);

		rowNamesButton.setTextRotation(-Math.PI / 2.0);

		add(topRightButton);
		add(bottomLeftButton);
		add(rowNamesButton);
		add(columnNamesButton);

		int fontSize = 20;
		topRightButton.setBounds(fontSize, fontSize, width - fontSize, height - fontSize);
		bottomLeftButton.setBounds(fontSize, fontSize, width - fontSize, height - fontSize);
		rowNamesButton.setBounds(0, fontSize, fontSize, height - fontSize);
		columnNamesButton.setBounds(fontSize, 0, width - fontSize, fontSize);
		addComponentListener(this);
		
		}

	public MatrixSymmetry getTriangles() {
		if (topRightButton.isSelected()){
			return bottomLeftButton.isSelected() ? MatrixSymmetry.ASYMMETRIC : MatrixSymmetry.SYMMETRIC_TOP;
		}
		return bottomLeftButton.isSelected() ? MatrixSymmetry.SYMMETRIC_BOTTOM : null;
	}
	
	public boolean hasRowNames(){
		return rowNamesButton.isSelected();
	}
	public boolean hasColumnNames(){
		return columnNamesButton.isSelected();
	}

	class ToggleAreaButton extends JToggleButton {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private double textAngle = 0;

		public ToggleAreaButton(String label) {
			super(label, true);
			Dimension size = getPreferredSize();
			size.width = size.height = Math.max(size.width, size.height);
			setPreferredSize(size);
			setContentAreaFilled(false);
			setRolloverEnabled(true);
		}

		public void setTextRotation(double angle) {
			this.textAngle = angle;
		}

		protected int[] getXPoints() {
			return new int[] { 0, getSize().width, getSize().width, 0 };
		}

		protected int[] getYPoints() {
			return new int[] { 0, 0, getSize().height, getSize().height };
		}
		
		protected void paintComponent(Graphics g) {
			Color color = Color.GRAY;
			if (getModel().isSelected()) {
				color = new Color(100, 200, 100);
			}
			if (this.getModel().isArmed())
				color = color.darker();
			else if (getModel().isRollover())
				color = color.brighter();
			
			g.setColor(color);
			int x[] = getXPoints();
			int y[] = getYPoints();
			g.fillPolygon(x, y, x.length);

		}

		@Override
		protected void paintBorder(Graphics g) {
			int x[] = getXPoints();
			int y[] = getYPoints();
			g.setColor(getForeground());
			g.drawPolygon(x, y, x.length);

			drawLabel((Graphics2D) g);
		}

		public void drawLabel(Graphics2D g) {
			int x[] = getXPoints();
			int y[] = getYPoints();
			int xMean = 0, yMean = 0;
			for (int i = 0; i < x.length; i++) {
				xMean += x[i];
				yMean += y[i];
			}

			xMean /= x.length;
			yMean /= y.length;

			String s = getText();

			Font font = g.getFont();

			Rectangle2D textRect = g.getFontMetrics().getStringBounds(s, g);
			int textWidth = (int) textRect.getWidth(), textHeight = (int) textRect.getHeight();
			int tx = xMean - (textWidth / 2);
			int ty = yMean + (textHeight / 2);

			if (textAngle != 0) {
				AffineTransform fontAT = new AffineTransform();
				fontAT.rotate(textAngle);
				font = font.deriveFont(fontAT);
				tx = xMean + (textHeight / 2);
				ty = yMean + (textWidth / 2);
			}

			if (!isSelected()) {
				@SuppressWarnings("unchecked")
				Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) font.getAttributes();
				attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
				font = new Font(attributes);
			}
			g.setFont(font);
			g.drawString(s, tx, ty);

		}

		Polygon polygon;

		@Override
		public boolean contains(int x, int y) {
			int xP[] = getXPoints();
			int yP[] = getYPoints();

			if (polygon == null || !polygon.getBounds().equals(getBounds())) {
				polygon = new Polygon(xP, yP, xP.length);
			}
			return polygon.contains(x, y);
		}

	}

	class ToggleTriangleAreaButton extends ToggleAreaButton {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4237673574772967581L;
		private boolean top;

		public ToggleTriangleAreaButton(String label, boolean top) {
			super(label);

			this.top = top;
		}

		@Override
		protected int[] getXPoints() {
			return new int[] { 0, top ? getSize().width : 0, getSize().width };
		}

		@Override
		protected int[] getYPoints() {
			return new int[] { 0, top ? 0 : getSize().height, getSize().height };
		}

	}

	public void setButtons(boolean rowNames, boolean columnNames, Boolean topHalf, Boolean bottomHalf) {
		if (topHalf != null)
			topRightButton.setSelected(topHalf);
		if (bottomHalf != null)
			bottomLeftButton.setSelected(bottomHalf);
		
		rowNamesButton.setSelected(rowNames);
		columnNamesButton.setSelected(columnNames);
	}

	@Override
	public void componentResized(ComponentEvent e) {
		int width = getWidth();
		int height = getHeight();
		int mid = Math.min(width, height);
		int fontSize = 20;
		mid = Math.max(mid, 150);
		
		int x = (width / 2) - (mid / 2);
		topRightButton.setBounds(x + fontSize, fontSize, mid - fontSize, mid - fontSize);
		bottomLeftButton.setBounds(x + fontSize, fontSize, mid - fontSize, mid - fontSize);
		rowNamesButton.setBounds(x, fontSize, fontSize, mid - fontSize);
		columnNamesButton.setBounds(x + fontSize, 0, mid - fontSize, fontSize);
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

}
