/*
 * Copyright (C) 2014 Raydac Research Group Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.igormaznitsa.zxpspritecorrector.tools;

import com.igormaznitsa.zxpspritecorrector.components.EditorComponent;
import com.igormaznitsa.zxpspritecorrector.components.ZXColorSelector;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import org.picocontainer.annotations.Inject;

public class ToolPencil extends AbstractTool {

  private static final long serialVersionUID = 1486692252806983383L;

  @Inject
  private ZXColorSelector colorSelector;

  public ToolPencil() {
    super("pencil.png", "Pencil allows to set pixels of defined color");
  }

  @Override
  public void process(final EditorComponent editComponent, final Rectangle area, final int modifiers) {
    final EditorComponent.ZXGraphics gfx = editComponent.getZXGraphics();

    final int index;

    if ((modifiers & MouseEvent.BUTTON1_MASK) != 0) {
      index = editComponent.isMode512() ? 1 : colorSelector.getSelectedInk();
    }
    else if ((modifiers & MouseEvent.BUTTON3_MASK) != 0) {
      index = editComponent.isMode512() ? 0 : colorSelector.getSelectedPaint();
    }
    else {
      return;
    }

    for (int x = 0; x < area.width; x++) {
      for (int y = 0; y < area.height; y++) {
        final int dx = x + area.x;
        final int dy = y + area.y;
        if (!isCoordValid(dx, dy)) {
          continue;
        }
        gfx.setPoint(dx, dy, index);
      }
    }

    gfx.flush();
  }
}
