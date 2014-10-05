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

package com.igormaznitsa.zxpspritecorrector.files;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import java.io.*;
import java.util.*;

public class TRDPlugin extends AbstractFilePlugin {

  public static final JBBPParser CATALOG_PARSER = JBBPParser.prepare("byte [8] name; ubyte type; <ushort start; <ushort length; ubyte sectors; ubyte firstSector; ubyte track;");
  
  private static final class TRDosCatalogItem {
    @Bin(type = BinType.BYTE_ARRAY) String name;
    @Bin(type = BinType.UBYTE) char type;
    @Bin(type = BinType.USHORT) int start;
    @Bin(type = BinType.USHORT) int length;
    @Bin(type = BinType.UBYTE) int sectors;
    @Bin(type = BinType.UBYTE) int firstSector;
    @Bin(type = BinType.UBYTE) int track;
  }
  
  public TRDPlugin(){
    super();
  }

  @Override
  public String getName() {
    return "TRD files";
  }

  @Override
  public String getToolTip() {
    return "A TR-DOS disk image format";
  }

  @Override
  public boolean hasInsideFileList() {
    return true;
  }

  @Override
  public List<Info> getInsideFileList(final File file) {
    try{
      final List<Info> result = new ArrayList<Info>();
      
      JBBPBitInputStream in = null;
      try{
        in = new JBBPBitInputStream(new FileInputStream(file));
        
        for(int i=0;i<128;i++){
          final TRDosCatalogItem item = CATALOG_PARSER.parse(in).mapTo(TRDosCatalogItem.class);
          if (item.name.charAt(0)>1){
            result.add(new Info(item.name, item.type, item.start, item.length));
          }
        }
        
      }finally{
        JBBPUtils.closeQuietly(in);
      }
      
      return result;
    }catch(Exception ex){
      return null;
    }
  }

  @Override
  public ReadResult readFrom(final File file, final int index) throws IOException {
    return null;
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data, final SessionData session) throws IOException {
  }

  @Override
  public boolean accept(final File pathname) {
    return pathname!= null && pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".trd");
  }

  @Override
  public String getDescription() {
    return getToolTip()+" (*.TRD)";
  }

  @Override
  public String getUID() {
    return "TRDP";
  }
  
}