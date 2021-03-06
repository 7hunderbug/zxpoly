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
package com.igormaznitsa.zxpoly.formats;

import com.igormaznitsa.zxpoly.components.*;
import static com.igormaznitsa.zxpoly.components.ZXPoly.PORTw_ZXPOLY_BLOCK;
import java.io.IOException;
import javax.swing.filechooser.FileFilter;

public abstract class Snapshot extends FileFilter {
  
  public void doMode48(final Motherboard board){
    board.set3D00(PORTw_ZXPOLY_BLOCK, true);
    for (final ZXPolyModule m : board.getZXPolyModules()) {
      m.lockZX48Mode();
    }
    board.getCPU0().doReset();
  }
  
  public void doMode128(final Motherboard board){
    board.set3D00(PORTw_ZXPOLY_BLOCK, true);
    board.getCPU0().doReset();
  }
  
  public abstract void loadFromArray(Motherboard board, final VideoController vc, byte [] array) throws IOException; 
  
  public abstract String getName();
}
