/*
 * Draw a Hidato Grid.
 * Author: Chris Reedy (Chris.Reedy@wwu.edu)
 * This work is licensed under the Creative Commons Attribution-NonCommercial 3.0
 * Unported License. To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-nc/3.0/ or send a letter to Creative
 * Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.File;

/* Class DrawGrid
 *
 * Draw and update a Kakuro Grid.
 *
 * To use the class:
 *   1. Call the constructor DrawGrid(int h, int w) which creates a
 *      grid with the given height (h) and width(w).
 *
 *   2. For every cell in the grid call one of:
 *     * fixedCell(int r, int c, int value) declaring that the cell at
 *       coordinates (r, c) (r = row, c = column, 0 <= r < h, 0 <= c < w)
 *       has the given fixed value.
 *     * valueCell(int r, int c) declaring that the cell at coordinates
 *       (r, c) is a cell where a value is to be filled in.
 *     * emptyCell(int r, int c) declaring that the cell at coordinates
 *       (r, c) will be empty, that is, not part of the grid.
 *      Note: If no call is made to one of the above functions for a given
 *      (r, c), the cell will be treated as an empty cell.
 * 
 *   3. Once step 2 is complete, call draw() to draw the grid.
 *
 *   4. While searching,
 *     * call fillCell(int r, int c, int n) to indicate that the value
 *       n should be filled in the cell at (r, c). (r, c) must be the
 *       location of a value cell from a call to valueCell in step 2.
 *     * call clearCell(int r, int c) to clear a previous value from
 *       the cell at (r, c). Again, (r, c) must be a value cell.
 *
 * Here is a rough example of how to use the class:
 *
 *   int height = <<height of grid>>;
 *   int width = <<width of grid>>;
 
 *   DrawGrid g = new DrawGrid(height, width);
 
 *   for (int row = 0; row < height; row++) {
 *     for (int col = 0; col < width; col++) {
 *       if (this cell has a fixed value) {
 *         g.fixedCell(row, col, value);
 *       } else if (this cell is to be filled) {
 *         g.valueCell(row, col);
 *       } else {
 *         g.emptyCell(row, col);
 *       }
 *     }
 *   }
 *
 *   g.draw();
 *
 *   search the grid {
 *     g.fillCell(r, c, n); // Places the value n at (r, c)
 *     g.clearCell(r, c); // Removes the value at (r, c)
 *
 * Note: After filling the cell at (r, c) using fillCell, the drawing
 * will delay for a fixed time. A call to g.setDelay(int delayTime) will
 * set the delay time to the given number of milliseconds. So, a one
 * second delay after each step is obtained by calling g.setDelay(1000).
 * The default delay is 50 milliseconds.
 */
public class DrawGrid {

   // Constants for Color for the grid
   private static final Color WALL_COLOR = Color.BLACK;
   private static final Color BACKGROUND = new Color(0.9f, 0.9f, 0.9f);
   private static final Color CELL_BACKGROUND = Color.WHITE;
   private static final Color FIXED_BACKGROUND = BACKGROUND;
   private static final Color EMPTY_COLOR = WALL_COLOR;
   private static final Color FIXED_COLOR = Color.BLACK;
   private static final Color VALUE_COLOR = Color.RED;
   
   // Constants for Font used for numbers in the grid
   private static final String FONT = "SansSerif";   
   private static final int FONT_STYLE = Font.PLAIN;
   private static final int valueFontSize = 28;
   
   // Width of border in pixels
   private static final int BORDER_WIDTH = 15;
   private static final int CELL_SIZE = 46;   
   private static final int EDGE_WIDTH = 5;

   // Cell locations for positioning values and sums in the cell
   private static final int cellCenter = CELL_SIZE / 2;
      
   // Time to sleep after a move (in milliseconds)
   private int sleepTime = 50;
   
   /* Set the delay time.
    *
    * The delay time is set to the value of the parameter delayTime.
    * If delay is negative, the time is set to zero.
    *
    * Pre: Draw grid object has been constructed.
    * Post: Delay time is set to specified value or zero
    *   if delayTime < 0.
    */
   public void setDelay(int delayTime) {
      if (delayTime < 0) {
         sleepTime = 0;
      } else {
         sleepTime = delayTime;
      }
   }
   
   /* Create a DrawGrid object.
    *
    * Pre: w and h are ints with 2 <= w, h <= 20
    * Post: DrawGrid object is constructed.
    *
    * An IllegalArgumentException is thrown if w or h is out of bounds.
    */
   public DrawGrid(int h, int w) {
      if ((w < 2 || 20 < w)) {
         throw new IllegalArgumentException("Grid width out of bounds: " + w);
      }
      if ((h < 2 || 20 < h)) {
         throw new IllegalArgumentException("Grid height out of bounds: " + h);
      } 
      this.width = w;
      this.height = h;
      grid = new GridCell[width][height];
      for (int c = 0; c < width; c++)
         for (int r = 0; r < height; r++)
            grid[c][r] = new GridCell();
            
      panel = new DrawingPanel(2 * BORDER_WIDTH + CELL_SIZE * width + 1,
                               2 * BORDER_WIDTH + CELL_SIZE * height + 1);
      
      valueFont = new Font(FONT, FONT_STYLE, valueFontSize);
      initGlyphData();     
   }
   
   /* Declare a cell as a fixed cell.
    *
    * Pre: DrawGrid object has been constructed.
    * Post: The cell at (r, c) is set to be a fixed cell with
    *   the given value.
    */
   public void fixedCell(int r, int c, int value) {
      assert 0 <= r && r < height;
      assert 0 <= c && c < width;
      GridCell gc = new GridCell();
      grid[c][r] = gc;
      gc.class_ = CellClass.FIXED;
      gc.value = value;   
   }
   
   /* Declare a cell as a value cell.
    *
    * Pre: DrawGrid object has been constructed.
    * Post: The cell at (r, c) is set to be a value cell.
    */
   public void valueCell(int r, int c) {
      assert 0 <= r && r < height;
      assert 0 <= c && c < width;
      GridCell gc = new GridCell();
      grid[c][r] = gc;
      gc.class_ = CellClass.VALUE;
      gc.value = 0;  
   }
   
   /* Declare a cell as an empty cell.
    *
    * Pre: DrawGrid object has been constructed.
    * Post: The cell at (r, c) is set to be a an empty cell.
    */
   public void emptyCell(int r, int c) {
      assert 0 <= r && r < height;
      assert 0 <= c && c < width;
      
      GridCell gc = new GridCell();
      grid[c][r] = gc;
      gc.class_ = CellClass.EMPTY;
   }
   
   /* Draw a grid.
    *
    * This draws a grid that has been initialized by calls to valueCell
    * and sumCell.
    *
    * Pre: DrawGrid object has been created and all cells have been
    *   initialized by calls to valueCell or sumCell.
    * Post: The grid is drawn on the display.
    */
   public void draw() {
      markExterior(); // Mark EMPTY cells on grid exterior
   
      panel.clear();
      panel.setBackground(BACKGROUND);
      pen = panel.getGraphics();

      // pen.setColor(CELL_BACKGROUND);
      // pen.fillRect(cellLeft(0), cellTop(0), width * CELL_SIZE - 1, height * CELL_SIZE - 1);
            
      for (int r = 0; r < height; r++) {
         for (int c = 0; c < width; c++) {
            GridCell gc = grid[c][r];
            switch (gc.class_) {
               case FIXED:
                  fillValueCell(r, c, FIXED_COLOR, FIXED_BACKGROUND);
                  drawEdges(r, c);
                  break;
               case VALUE:
                  if (gc.value != 0) {
                     fillValueCell(r, c, VALUE_COLOR, CELL_BACKGROUND);
                  } else {
                     clearCell(r, c);
                  }
                  drawEdges(r, c);
                  break;
               case EMPTY:
                  drawEmptyCell(r, c, gc.exterior);
                  break;
            }
         }
      }
   }
   
   /* Fill the given cell with the value n with the given color.
    *
    * Pre: The grid has been constructed, fully initialized and draw()
    *   has been called at least once.
    * Post: The cell at (r, c) is filled with the value n and the
    *   has slept for the given sleep time.
    */
   public void fillCell(int r, int c, int n) {
      assert 0 <= r && r < height;
      assert 0 <= c && c < width;
      assert 1 <= n && n <= width * height;
      GridCell gc = grid[c][r];
      assert gc.class_ == CellClass.VALUE || gc.class_ == CellClass.FIXED;
      gc.value = n;
      fillValueCell(r, c, VALUE_COLOR, CELL_BACKGROUND);
      panel.sleep(sleepTime);
   }
      
   /* Clear the given cell.
    *
    * Pre: The grid has been constructed, fully initialized and draw()
    *   has been called at least once.
    * Post: The cell at (r, c) is cleared of any previous value.
    */
   public void clearCell(int r, int c) {
      assert 0 <= r && r < height;
      assert 0 <= c && c < width;
      GridCell gc = grid[c][r];
      assert gc.class_ == CellClass.VALUE;
      gc.value = 0;
      
      pen.setColor(CELL_BACKGROUND);
      pen.fillRect(cellLeft(c), cellTop(r), CELL_SIZE - 1, CELL_SIZE - 1);
   }

   // The three different types of grid cells
   private static enum CellClass { EMPTY, FIXED, VALUE };
   
   // Structure for storing the type and contents of a grid cell.
   private static class GridCell {
      CellClass class_;
      int value; // 0 == not-specified, 1-n are values
      boolean exterior; // true for EMPTY cells on grid exterior
      
      GridCell() {
         class_ = CellClass.EMPTY;
         value = 0;
         exterior = false;
      }
   }
   
   // The grid
   private int height; // Height of the grid
   private int width; // Width of the grid
   private GridCell[][] grid; // Contents of the grid.
   
   // Mark EMPTY cells on the grid exterior as exterior
   private void markExterior() {
      for (GridCell[] row : grid) {
         for(GridCell cell : row) {
            if (cell.class_ == CellClass.EMPTY) {
               cell.exterior = false;
            }
         }
      }
      
      for (int r = 0; r < height; r++) {
         checkGridCell(0, r);
         checkGridCell(width - 1, r);
      }
      for (int c = 0; c < width; c++) {
         checkGridCell(c, 0);
         checkGridCell(c, height - 1);
      }
   }
   
   private void checkGridCell(int c, int r) {
      if (0 <= c && c < width && 0 <= r && r < height) {
         GridCell cell = grid[c][r];
         if (cell.class_ == CellClass.EMPTY && !cell.exterior) {
            cell.exterior = true;
            checkGridCell(c - 1, r);
            checkGridCell(c + 1, r);
            checkGridCell(c, r - 1);
            checkGridCell(c, r + 1);
         }
      }
   }

   // The drawing panel used to draw the grid
   private DrawingPanel panel = null;
   private Graphics2D pen = null;
   
   // Utility routines.
   // celltop computes the y coordinate for the top of a cell in row r
   // (rows are numbered starting at zero.)  cellleft computes the x
   // coordinate for the left of a cell in column c (columns are also
   // numbered from zero. (cellleft(c), celltop(r)) gives the (x, y)
   // coordinates of the top left of a cell.
   private int cellTop(int r) {
      return BORDER_WIDTH + CELL_SIZE * r;
   }
   
   private int cellLeft(int c) {
      return BORDER_WIDTH + CELL_SIZE * c;
   }
   
   // Put a value in a cell
   private void fillValueCell(int r, int c, Color valueColor, Color bkgColor) {
      GridCell gc = grid[c][r];
      int x0 = cellLeft(c);
      int y0 = cellTop(r);
      int size = CELL_SIZE - 3;
      pen.setColor(bkgColor);
      pen.fillRect(x0, y0, size, size);
      pen.setColor(valueColor);
      pen.setFont(valueFont);
      GlyphData gd = valueGlyphs[gc.value];
      float x = x0 + cellCenter + gd.centerX;
      float y = y0 + cellCenter + gd.centerY;
      pen.drawGlyphVector(gd.gv, x, y);
   }
   
   // Draw an empty cell
   private void drawEmptyCell(int r, int c, boolean exterior) {
      if (!exterior) {
         int x0 = cellLeft(c) - 1;
         int y0 = cellTop(r) - 1;
         int size = CELL_SIZE + 1;
         pen.setColor(EMPTY_COLOR);
         pen.fillRect(x0, y0, size, size);
      }
   }
   
   // Check to see if a cell is empty for drawing purposes
   boolean drawEmpty(int c, int r) {
      return r < 0 || r >= height || c < 0 || c >= width
            || (grid[c][r].class_ == CellClass.EMPTY &&
                  grid[c][r].exterior);
   }
   
   // Place the edges on a cell which is either fixed or value
   void drawEdges(int r, int c) {
      int x0 = cellLeft(c);
      int y0 = cellTop(r);
      int x1 = x0 + CELL_SIZE;
      int y1 = y0 + CELL_SIZE;
      
      pen.setColor(WALL_COLOR);
      // Draw top edge
      if (drawEmpty(c, r - 1)) {
         if (drawEmpty(c - 1, r - 1)) {
            pen.fillRect(x0 - EDGE_WIDTH, y0 - EDGE_WIDTH, CELL_SIZE + EDGE_WIDTH, EDGE_WIDTH);
         } else {
            pen.fillRect(x0, y0 - EDGE_WIDTH, CELL_SIZE, EDGE_WIDTH);
         }
      } else {
         pen.drawLine(x0, y0 - 1, x1, y0 - 1);
      }
      
      // Draw left edge
      if (drawEmpty(c - 1, r)) {
         pen.fillRect(x0 - EDGE_WIDTH, y0, EDGE_WIDTH, CELL_SIZE);               
      } else {
         pen.drawLine(x0 - 1, y0, x0 - 1, y1);
      }
      
      // Draw bottom edge maybe
      if (drawEmpty(c, r + 1)) {
         int x = x0;
         int len = CELL_SIZE - 1;
         if (drawEmpty(c - 1, r + 1)) {
            x -= EDGE_WIDTH;
            len += EDGE_WIDTH;
         }
         if (drawEmpty(c + 1, r + 1)) {
            len += EDGE_WIDTH;
         }
         pen.fillRect(x, y1 - 1, len, EDGE_WIDTH);
      }
      
      // Draw right edge maybe
      if (drawEmpty(c + 1, r)) {
         int y = y0;
         int len = CELL_SIZE - 1;
         if (drawEmpty(c + 1, r - 1)) {
            y -= EDGE_WIDTH;
            len += EDGE_WIDTH;
         }
         pen.fillRect(x1 - 1, y, EDGE_WIDTH, len);
      }      
   }

//       int x0 = cellLeft(0);
//       int y0 = cellTop(0);
//       int x1 = cellLeft(width);
//       int y1 = cellTop(height);
// 
//       pen.setColor(WALL_COLOR);
//       int xw = x1 - x0 + 2 * EDGE_WIDTH - 1;
//       int yw = y1 - y0 + 2 * EDGE_WIDTH - 1;
//       pen.fillRect(x0 - EDGE_WIDTH, y0 - EDGE_WIDTH, xw, EDGE_WIDTH);
//       pen.fillRect(x0 - EDGE_WIDTH, y1 - 1, xw, EDGE_WIDTH);
//       pen.fillRect(x0 - EDGE_WIDTH, y0 - EDGE_WIDTH, EDGE_WIDTH, yw);
//       pen.fillRect(x1 - 1, y0 - EDGE_WIDTH, EDGE_WIDTH, yw);
//       
//       pen.setColor(WALL_COLOR);
//       for (int r = 1; r < height; r++) {
//          int y = cellTop(r) - 1;
//          pen.drawLine(x0, y, x1, y);
//       }
//       for (int c = 1; c < width; c++) {
//          int x = cellLeft(c) - 1;
//          pen.drawLine(x, y0, x, y1);
//       }
      
   
   // Local storage for fonts for numbers
   private Font valueFont;
   
   // Structure for storing data associated with a GlyphVector. This
   // is precomputed and used to speed up the drawing of the numbers
   // in the grid.
   private static class GlyphData {
      float centerX; // X and Y to be added to the center coordinates 
      float centerY; // to center the glyph at those coordinates
      GlyphVector gv;
      
      GlyphData(float cx, float cy, GlyphVector gv) {
         this.centerX = cx;
         this.centerY = cy;
         this.gv = gv;
      }
   }
   
   // Storage for precomputed GlyphData
   private GlyphData[] valueGlyphs;

   // Initialize the GlyphData   
   private void initGlyphData() {
      FontRenderContext frc = panel.getGraphics().getFontRenderContext();

      // Generate Glyph Vectors for values
      int numGlyphs = 1 + width * height;
      valueGlyphs = new GlyphData[numGlyphs];
      for (int n = 1; n < numGlyphs; n++ ) {
         String ds = Integer.toString(n);
         GlyphVector gv = valueFont.createGlyphVector(frc, ds);
         Rectangle2D r = gv.getVisualBounds();
         valueGlyphs[n] = new GlyphData(
               (float)(-r.getCenterX()), (float)(-r.getCenterY()), gv);
      }
   }
}
