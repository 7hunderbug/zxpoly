package com.igormaznitsa.zxpspritecorrector.components;

import com.igormaznitsa.zxpspritecorrector.utils.ZXPalette;
import java.awt.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public final class EditorComponent extends JComponent implements SpinnerModel {

  public enum ShowAttributes {

    DONT_SHOW,
    SHOW_BASE,
    SHOW_512x384_ZXPOLY_PLANES
  }

  private static final long serialVersionUID = -6948149982924499351L;

  private static final Stroke GRID_STROKE = new BasicStroke(0.3f);
  private static final Stroke COLUMN_BORDER_STROKE = new BasicStroke(0.7f);
  private static final Stroke TOOL_AREA_STROKE = new BasicStroke(2.3f);

  private Color colorToolArea = Color.WHITE;

  private Color colorPixelOn = Color.GRAY.darker();
  private Color colorPixelOff = Color.DARK_GRAY.darker();
  private Color colorZX512On = Color.YELLOW;
  private Color colorZX512Off = Color.BLUE;
  private Color colorGrid = Color.ORANGE;
  private Color colorColumnBorder = Color.CYAN;

  private BufferedImage image;
  private boolean mode512;

  private boolean invertShowBaseData;
  private boolean showColumnBorders;
  private boolean showGrid;
  private ShowAttributes showAttributes = ShowAttributes.DONT_SHOW;
  private boolean addressingModeZXScreen;
  private Dimension preferredSize;
  private int zoom = 1;
  private int columns = 32;
  private ZXPolyData processingData;
  private int startAddress;

  private Rectangle toolArea;

  private int gridStep = 1;

  private final ZXGraphics zxGraphics = new ZXGraphics();

  private final List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
  
  private final java.util.List<ZXPolyData.UndoBlock> listUndo = new ArrayList<ZXPolyData.UndoBlock>();
  private final java.util.List<ZXPolyData.UndoBlock> listRedo = new ArrayList<ZXPolyData.UndoBlock>();

  private static final RenderingHints RENDERING_IMAGE_HINTS = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
  private static final RenderingHints RENDERING_LINE_HINTS = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

  static {
    RENDERING_IMAGE_HINTS.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
    RENDERING_IMAGE_HINTS.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);

    RENDERING_LINE_HINTS.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
  }

  public Point mousePoint2ScreenPoint(final Point pointAtComponent) {
    if (pointAtComponent == null) {
      return null;
    }
    return new Point(pointAtComponent.x / this.zoom, pointAtComponent.y / this.zoom);
  }

  public class ZXGraphics {

    private ZXGraphics() {

    }

    private int coordToAddress(final int x, final int y) {
      final int result;

      if (processingData == null || x < 0 || y < 0) {
        result = -1;
      }
      else {
        final int dx = mode512 ? x >> 1 : x;
        final int dy = mode512 ? y >> 1 : y;

        final int theY = addressingModeZXScreen ? VideoMode.zxy2y(dy) : dy;
        final int rowAddress = theY * columns + startAddress;

        if (dx >= (columns << 3) || rowAddress >= processingData.length()) {
          result = -1;
        }
        else {
          result = (dx >>> 3) + rowAddress;
        }
      }

      return result;
    }

    private int makeXMask(final int x) {
      return 1 << (7 - (x & 0x7));
    }

    public ZXGraphics setPoint(final int x, final int y, final int cpu3012) {
      final int address = coordToAddress(x, y);
      if (address >= 0) {
        final int mask = processingData.getMask(address);

        final int packed3012 = processingData.getPackedZxPolyData3012(address);

        if (mode512) {
          final int bitmask = makeXMask(x >> 1);
          final int invertedbitmask = ~bitmask;

          final int value = (cpu3012 == 0 ? 0 : 0xFF) & bitmask;

          if ((x & 1) == 0) {
            if ((y & 1) == 0) {
              // CPU 0
              processingData.setZXPolyData(address, mask | bitmask,
                      ((packed3012 >>> 16) & invertedbitmask) | value,
                      packed3012 >>> 8,
                      packed3012,
                      packed3012 >>> 24);
            }
            else {
              // CPU 2
              processingData.setZXPolyData(address, mask | bitmask,
                      packed3012 >>> 16,
                      packed3012 >>> 8,
                      (packed3012 & invertedbitmask) | value,
                      packed3012 >>> 24);
            }
          }
          else {
            if ((y & 1) == 0) {
              // CPU 1
              processingData.setZXPolyData(address, mask | bitmask,
                      packed3012 >>> 16,
                      ((packed3012 >>> 8) & invertedbitmask) | value,
                      packed3012,
                      packed3012 >>> 24);
            }
            else {
              // CPU 3
              processingData.setZXPolyData(address, mask | bitmask,
                      packed3012 >>> 16,
                      packed3012 >>> 8,
                      packed3012,
                      ((packed3012 >>> 24) & invertedbitmask) | value);
            }
          }

        }
        else {
          final int bitmask = makeXMask(x);
          final int invertedbitmask = ~bitmask;
          processingData.setZXPolyData(address, mask | bitmask,
                  ((packed3012 >>> 16) & invertedbitmask) | (((cpu3012 & 4) == 0 ? 0 : 0xFF) & bitmask),
                  ((packed3012 >>> 8) & invertedbitmask) | (((cpu3012 & 2) == 0 ? 0 : 0xFF) & bitmask),
                  (packed3012 & invertedbitmask) | (((cpu3012 & 1) == 0 ? 0 : 0xFF) & bitmask),
                  ((packed3012 >>> 24) & invertedbitmask) | (((cpu3012 & 8) == 0 ? 0 : 0xFF) & bitmask)
          );
        }
      }

      return this;
    }

    public ZXGraphics resetPoint(final int x, final int y) {
      final int address = coordToAddress(x, y);
      if (address >= 0) {
        final int mask = processingData.getMask(address);

        final int packed3012 = processingData.getPackedZxPolyData3012(address);

        final int bitmask = makeXMask(x >> (mode512 ? 1 : 0));
        final int invertedbitmask = ~bitmask;
        processingData.setZXPolyData(address, mask & invertedbitmask,
                (packed3012 >>> 16) & invertedbitmask,
                (packed3012 >>> 8) & invertedbitmask,
                packed3012 & invertedbitmask,
                (packed3012 >>> 24) & invertedbitmask
        );
      }

      return this;
    }

    public int getPoint3012(final int x, final int y) {
      final int address = coordToAddress(x, y);

      int result = 0;

      if (address >= 0) {
        final int bitmask = makeXMask(x);

        if ((processingData.getMask(address) & bitmask) == 0) {
          result = 0;
        }
        else {
          final int packed3012 = processingData.getPackedZxPolyData3012(address);
          result = ((packed3012 & bitmask) == 0 ? 0 : 1)
                  | ((packed3012 & (bitmask << 8)) == 0 ? 0 : 2)
                  | ((packed3012 & (bitmask << 16)) == 0 ? 0 : 4)
                  | ((packed3012 & (bitmask << 24)) == 0 ? 0 : 8);
        }
      }

      return result;
    }

    public boolean isBaseBitSet(final int x, final int y) {
      final int address = coordToAddress(x, y);
      if (address >= 0) {
        return (processingData.getBaseData(address) & makeXMask(x)) != 0;
      }

      return false;
    }

    public void flush() {
      _updatePictureInBuffer();
      repaint();
    }
  }

  public ZXGraphics getZXGraphics() {
    return this.zxGraphics;
  }

  public void setToolArea(final Rectangle rect) {
    this.toolArea = rect;
    repaint();
  }

  public Rectangle getToolArea() {
    return this.toolArea;
  }

  public ZXPolyData getProcessingData() {
    return this.processingData;
  }

  public void setProcessingData(final ZXPolyData data) {
    this.listRedo.clear();
    this.listUndo.clear();

    this.processingData = data;
    _updatePictureInBuffer();
    repaint();
  }

  public void setColumns(final int value) {
    this.columns = Math.max(1, Math.min(32, value));
    _updatePictureInBuffer();
    repaint();
  }

  public int getColumns() {
    return this.columns;
  }

  public void setInvertShowBaseData(final boolean flag) {
    this.invertShowBaseData = flag;
    _updatePictureInBuffer();
    repaint();
  }

  public boolean isInvertShowBaseData() {
    return this.invertShowBaseData;
  }

  public void setShowColumnBorders(final boolean flag) {
    this.showColumnBorders = flag;
    repaint();
  }

  public boolean hasUndo() {
    return !this.listUndo.isEmpty();
  }

  public boolean hasRedo() {
    return !this.listRedo.isEmpty();
  }

  public void addUndo() {
    if (this.processingData != null) {
      this.listRedo.clear();
      this.listUndo.add(this.processingData.makeUndo());
      while (this.listUndo.size() > 15) {
        this.listUndo.remove(0);
      }
    }
  }

  public void undo() {
    if (this.processingData != null && !this.listUndo.isEmpty()) {
      final ZXPolyData.UndoBlock blockPrevious = this.listUndo.remove(this.listUndo.size() - 1);
      if (this.listRedo.isEmpty()) {
        this.listRedo.add(this.processingData.makeUndo());
      }

      this.listRedo.add(blockPrevious);
      this.processingData.restoreFromUndo(blockPrevious);
      _updatePictureInBuffer();
      repaint();
    }
  }

  public void redo() {
    if (this.processingData != null && !this.listRedo.isEmpty()) {
      final ZXPolyData.UndoBlock block = this.listRedo.remove(this.listRedo.size() - 1);
      this.listUndo.add(block);
      this.processingData.restoreFromUndo(block);
      _updatePictureInBuffer();
      repaint();
    }
  }

  public void clear() {
    if (this.processingData != null) {
      this.listRedo.clear();
      this.listUndo.clear();
      this.processingData.clear();
      _updatePictureInBuffer();
      repaint();
    }
  }

  public boolean isShowColumnBorders() {
    return this.showColumnBorders;
  }

  public boolean isShowGrid() {
    return this.showGrid;
  }

  public void setShowGrid(final boolean flag) {
    this.showGrid = flag;
    repaint();
  }

  public int getGridStep() {
    return this.gridStep;
  }

  public void setGridStep(final int step) {
    this.gridStep = Math.max(1, Math.min(128, step));
    repaint();
  }

  public void setAddress(final int address) {
    if (this.processingData == null) {
      this.startAddress = 0;
    }
    else {
      this.startAddress = Math.max(0, Math.min(this.processingData.length() - 1, address));
    }
  
    for(final ChangeListener l : this.changeListeners){
      l.stateChanged(new ChangeEvent(this));
    }
    
    _updatePictureInBuffer();
    repaint();
  }

  public int getAddress() {
    return this.startAddress;
  }

  @Override
  public boolean isFocusable() {
    return false;
  }

  public void setZXScreenMode(final boolean flag) {
    this.addressingModeZXScreen = flag;
    _updatePictureInBuffer();
    repaint();
  }

  public boolean isZXScreenMode() {
    return this.addressingModeZXScreen;
  }

  public Color getToolAreaColor() {
    return this.colorToolArea;
  }

  public void setToolAreaColor(final Color color) {
    this.colorToolArea = color;
    repaint();
  }

  public Color getColorPixelOn() {
    return colorPixelOn;
  }

  public void setColorPixelOn(final Color colorPixelOn) {
    this.colorPixelOn = colorPixelOn;
    repaint();
  }

  public Color getColorPixelOff() {
    return colorPixelOff;
  }

  public void setColorPixelOff(final Color colorPixelOff) {
    this.colorPixelOff = colorPixelOff;
    repaint();
  }

  public Color getColorZX512On() {
    return colorZX512On;
  }

  public void setColorZX512On(Color colorZX512On) {
    this.colorZX512On = colorZX512On;
    _updatePictureInBuffer();
    repaint();
  }

  public Color getColorZX512Off() {
    return colorZX512Off;
  }

  public void setColorZX512Off(Color colorZX512Off) {
    this.colorZX512Off = colorZX512Off;
    _updatePictureInBuffer();
    repaint();
  }

  public void setShowAttributes(final ShowAttributes selected) {
    this.showAttributes = selected;
    _updatePictureInBuffer();
    repaint();
  }

  public ShowAttributes getShowAttributes() {
    return this.showAttributes;
  }

  public Color getColorGrid() {
    return colorGrid;
  }

  public void setColorGrid(Color colorGrid) {
    this.colorGrid = colorGrid;
    repaint();
  }

  public Color getColorColumnBorder() {
    return colorColumnBorder;
  }

  public void setColorColumnBorder(Color colorColumnBorder) {
    this.colorColumnBorder = colorColumnBorder;
    repaint();
  }

  public boolean isMode512() {
    return this.mode512;
  }

  public void setMode512(final boolean flag) {
    this.mode512 = flag;

    final int width = flag ? 512 : 256;
    final int height = flag ? 384 : 192;

    final BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    this.image = newImage;
    _updatePictureInBuffer();
    _updatePreferredSize();

    revalidate();
    repaint();
  }

  public void setZoom(final int zoom){
    this.zoom = Math.max(1,Math.min(10,zoom));
    _updatePreferredSize();
    revalidate();
    repaint();
  }
  
  public int getZoom() {
    return this.zoom;
  }

  public void zoomIn() {
    this.zoom = Math.min(this.zoom + 1, 10);
    _updatePreferredSize();
    revalidate();
    repaint();
  }

  public void zoomOut() {
    this.zoom = Math.max(this.zoom - 1, 1);
    _updatePreferredSize();
    revalidate();
    repaint();
  }

  private void _updatePictureInBuffer() {
    final Graphics2D gfx = this.image.createGraphics();

    gfx.setColor(Color.black);
    gfx.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());

    if (this.processingData != null) {
      int column = 0;

      int x = 0;
      int y = 0;
      final int step = this.mode512 ? 2 : 1;

      for (int addr = this.startAddress; addr < this.processingData.length(); addr++) {
        int basedata = this.processingData.getBaseData(addr);
        int mask = this.processingData.getMask(addr);

        final int cury;
        if (this.addressingModeZXScreen) {
          cury = VideoMode.y2zxy(this.mode512 ? y >> 1 : y) << (this.mode512 ? 1 : 0);
        }
        else {
          cury = y;
        }

        final int packedData3012 = this.processingData.getPackedZxPolyData3012(addr);

        int data0 = packedData3012 >>> 16;
        int data1 = packedData3012 >>> 8;
        int data2 = packedData3012;
        int data3 = packedData3012 >>> 24;

        final int attributeAddress = ZXPalette.calcAttributeAddressZXMode(this.startAddress, addr - this.startAddress);
        final int baseAttribute = attributeAddress >= this.processingData.length() ? 0 : this.processingData.getBaseData(attributeAddress);

        for (int i = 0; i < 8; i++) {
          if ((mask & 0x80) == 0) {
            // point of base
            if (this.showAttributes == ShowAttributes.SHOW_BASE) {
              final Color inkColor = ZXPalette.extractInk(baseAttribute);
              final Color paperColor = ZXPalette.extractPaper(baseAttribute);
              gfx.setColor((basedata & 0x80) == 0 ? paperColor : inkColor);
            }
            else if (this.invertShowBaseData) {
              gfx.setColor((basedata & 0x80) == 0 ? this.colorPixelOn : this.colorPixelOff);
            }
            else {
              gfx.setColor((basedata & 0x80) == 0 ? this.colorPixelOff : this.colorPixelOn);
            }
            gfx.fillRect(x, cury, step, step);
          }
          else {
            // point of a zxpoly mode
            if (this.mode512) {
              // 512x384 mode
              if (this.showAttributes == ShowAttributes.SHOW_512x384_ZXPOLY_PLANES) {
                final int packedAttributes3012 = attributeAddress >= this.processingData.length() ? 0 : this.processingData.getPackedZxPolyData3012(attributeAddress);
                final int attr0 = (packedAttributes3012 >>> 16) & 0xFF;
                gfx.setColor((data0 & 0x80) == 0 ? ZXPalette.extractPaper(attr0) : ZXPalette.extractInk(attr0));
                gfx.drawLine(x, cury, x, cury);

                final int attr1 = (packedAttributes3012 >>> 8) & 0xFF;
                gfx.setColor((data1 & 0x80) == 0 ? ZXPalette.extractPaper(attr1) : ZXPalette.extractInk(attr1));
                gfx.drawLine(x + 1, cury, x + 1, cury);

                final int attr2 = packedAttributes3012 & 0xFF;
                gfx.setColor((data2 & 0x80) == 0 ? ZXPalette.extractPaper(attr2) : ZXPalette.extractInk(attr2));
                gfx.drawLine(x, cury + 1, x, cury + 1);

                final int attr3 = (packedAttributes3012 >>> 24) & 0xFF;
                gfx.setColor((data3 & 0x80) == 0 ? ZXPalette.extractPaper(attr3) : ZXPalette.extractInk(attr3));
                gfx.drawLine(x + 1, cury + 1, x + 1, cury + 1);
              }
              else {
                gfx.setColor((data0 & 0x80) == 0 ? this.colorZX512Off : this.colorZX512On);
                gfx.drawLine(x, cury, x, cury);
                gfx.setColor((data1 & 0x80) == 0 ? this.colorZX512Off : this.colorZX512On);
                gfx.drawLine(x + 1, cury, x + 1, cury);
                gfx.setColor((data2 & 0x80) == 0 ? this.colorZX512Off : this.colorZX512On);
                gfx.drawLine(x, cury + 1, x, cury + 1);
                gfx.setColor((data3 & 0x80) == 0 ? this.colorZX512Off : this.colorZX512On);
                gfx.drawLine(x + 1, cury + 1, x + 1, cury + 1);
              }
            }
            else {
              // zxpoly mode
              final int colorIndex = ((data3 & 0x80) >>> 4) | ((data0 & 0x80) >>> 5) | ((data1 & 0x80) >>> 6) | ((data2 & 0x80) >>> 7);
              gfx.setColor(ZXPalette.COLORS[colorIndex]);
              gfx.fillRect(x, cury, step, step);
            }
          }

          basedata <<= 1;
          data0 <<= 1;
          data1 <<= 1;
          data2 <<= 1;
          data3 <<= 1;
          mask <<= 1;
          x += step;
        }

        column++;
        if (column >= this.columns) {
          x = 0;
          column = 0;
          y += step;
          if (y >= this.image.getHeight()) {
            break;
          }
        }
      }
    }
    gfx.dispose();
  }

  private void _updatePreferredSize() {
    this.preferredSize = new Dimension(getWidth(), getHeight());
  }

  @Override
  public Dimension getPreferredSize() {
    return this.preferredSize;
  }

  @Override
  public Dimension getMinimumSize() {
    return this.preferredSize;
  }

  @Override
  public Dimension getMaximumSize() {
    return this.preferredSize;
  }

  @Override
  public int getWidth() {
    return this.image.getWidth() * this.zoom;
  }

  @Override
  public int getHeight() {
    return this.image.getHeight() * this.zoom;
  }

  public EditorComponent() {
    super();
    setMode512(false);
  }

  @Override
  public void paintComponent(final Graphics g) {
    final Graphics2D gfx = (Graphics2D) g;
    gfx.setRenderingHints(RENDERING_IMAGE_HINTS);

    final Dimension size = this.getPreferredSize();

    gfx.drawImage(this.image, 0, 0, size.width, size.height, null);

    if (this.zoom > 1) {
      gfx.setRenderingHints(RENDERING_LINE_HINTS);
      final int columnBorder = this.columns * (this.zoom << (this.mode512 ? 4 : 3));

      if (this.showGrid) {

        gfx.setStroke(GRID_STROKE);
        gfx.setColor(this.colorGrid);
        final int step = this.gridStep * this.zoom * (this.mode512 ? 2 : 1);
        for (int i = 0; i <= columnBorder; i += step) {
          gfx.drawLine(i, 0, i, size.height);
        }
        for (int i = 0; i < size.height; i += step) {
          gfx.drawLine(0, i, columnBorder, i);
        }
      }

      if (this.showColumnBorders) {
        final int step = this.zoom << (this.mode512 ? 4 : 3);
        gfx.setStroke(COLUMN_BORDER_STROKE);
        gfx.setColor(this.colorColumnBorder);
        for (int i = 0; i <= columnBorder; i += step) {
          gfx.drawLine(i, 0, i, size.height);
        }
      }
    }

    if (this.toolArea != null) {
      gfx.setRenderingHints(RENDERING_LINE_HINTS);
      gfx.setStroke(TOOL_AREA_STROKE);
      gfx.setColor(this.colorToolArea);
      gfx.drawRect(this.toolArea.x * this.zoom, this.toolArea.y * this.zoom, this.toolArea.width * this.zoom, this.toolArea.height * this.zoom);
      gfx.setColor(this.colorToolArea.darker().darker().darker());
      gfx.drawRect(this.toolArea.x * this.zoom - 1, this.toolArea.y * this.zoom - 1, this.toolArea.width * this.zoom + 1, this.toolArea.height * this.zoom + 1);
    }
  }

  public boolean hasData() {
    return this.processingData != null;
  }

  public void copyPlansFromBase() {
    if (this.processingData!=null){
      this.processingData.copyPlansFromBase();
      _updatePictureInBuffer();
      repaint();
    }
  }

  @Override
  public Object getValue() {
    return this.getAddress();
  }

  @Override
  public void setValue(final Object addr) {
    final int address = (Integer)addr;
    setAddress(address);
  }

  @Override
  public Object getNextValue() {
    if (hasData()){
      return Math.min(this.getProcessingData().length()-1, this.startAddress+1);
    }else{
      return 0;
    }
  }

  @Override
  public Object getPreviousValue() {
    if (hasData()) {
      return Math.max(0, this.startAddress - 1);
    }
    else {
      return 0;
    }
  }

  @Override
  public void addChangeListener(final ChangeListener l) {
    this.changeListeners.add(l);
  }

  @Override
  public void removeChangeListener(final ChangeListener l) {
    this.changeListeners.remove(l);
  }
}
