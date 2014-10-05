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

import com.igormaznitsa.zxpspritecorrector.MainFrame;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import java.io.*;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;
import org.picocontainer.annotations.Inject;

public abstract class AbstractFilePlugin extends FileFilter {
  public final static class ReadResult {
    private final ZXPolyData data;
    private final SessionData session;

    public ReadResult(final ZXPolyData data, final SessionData session) {
      this.data = data;
      this.session = session;
    }
  
    public ZXPolyData getData(){
      return this.data;
    }
    
    public SessionData getSessionData(){
      return this.session;
    }
    
  }
  
  @Inject
  protected MainFrame mainFrame;
  
  public AbstractFilePlugin(){
    super();
  }

  public abstract String getName();
  public abstract String getToolTip();
  public abstract boolean hasInsideFileList();
  
  public String getFileInfo(File file){
    return "";
  }
  
  public abstract String getUID();
  
  public abstract List<Info> getInsideFileList(File file);

  public abstract ReadResult readFrom(File file, int index) throws IOException;
  public abstract void writeTo(File file, ZXPolyData data, SessionData sessionData) throws IOException;

  public boolean saveDataToFile(final File file, final byte [] data) throws IOException {
    if (file.isFile()){
      switch(JOptionPane.showConfirmDialog(this.mainFrame, "Overwrite file '"+file.getAbsolutePath()+"'?","Overwrite file",JOptionPane.YES_NO_CANCEL_OPTION)){
        case JOptionPane.NO_OPTION : return true;
        case JOptionPane.CANCEL_OPTION : return false;
      }
    }
    FileUtils.writeByteArrayToFile(file, data);
    return true;
  }
}