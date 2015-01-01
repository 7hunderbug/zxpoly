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
package com.igormaznitsa.zxpoly;

import com.igormaznitsa.zxpoly.components.*;
import com.igormaznitsa.zxpoly.components.betadisk.TRDOSDisk;
import com.igormaznitsa.zxpoly.formats.*;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;

public class MainForm extends javax.swing.JFrame implements Runnable {
//  private static final long CYCLES_BETWEEN_INT = 8000000000000L;
  private static final long CYCLES_BETWEEN_INT = 64000L;

  
  private static class TRDFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".trd");
    }

    @Override
    public String getDescription() {
      return "TR-DOS image (*.trd)";
    }

  }

  private static class SNAFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".sna");
    }

    @Override
    public String getDescription() {
      return "Z80 Snapshot (*.sna)";
    }

  }

  private static final long serialVersionUID = 7309959798344327441L;
  public static final Logger log = Logger.getLogger("UI");

  private final Motherboard board;
  private final long SCREEN_REFRESH_DELAY = 100L;

  private final ReentrantLock stepSemaphor = new ReentrantLock();

  private class KeyboardDispatcher implements KeyEventDispatcher {

    private final KeyboardAndTape keyboard;

    public KeyboardDispatcher(final KeyboardAndTape kbd) {
      this.keyboard = kbd;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
      this.keyboard.onKeyEvent(e);
      return false;
    }
  }

  public MainForm(final String romResource) throws IOException {
    initComponents();
    log.info("Loading test rom [" + romResource + ']');
    final RomData rom = RomData.read(Utils.findResourceOrError("com/igormaznitsa/zxpoly/rom/" + romResource));
    this.board = new Motherboard(rom);
    log.info("Main form completed");
    this.board.reset();

    this.scrollPanel.getViewport().add(this.board.getVideoController());

    final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyboardDispatcher(this.board.getKeyboard()));

    final Thread daemon = new Thread(this, "ZXPolyThread");
    daemon.setDaemon(true);
    daemon.start();

    pack();
  }

  @Override
  public void run() {
    long nextSystemInt = System.currentTimeMillis() + 20;
    long nextScreenRefresh = System.currentTimeMillis() + SCREEN_REFRESH_DELAY;

    while (!Thread.currentThread().isInterrupted()) {
      stepSemaphor.lock();
      try {
        final boolean intsignal;

        if (nextSystemInt <= System.currentTimeMillis()) {
          intsignal = true;
          nextSystemInt = System.currentTimeMillis() + 20;
          this.board.getCPU0().resetTactCounter();
        }
        else {
          intsignal = false;
        }

        this.board.step(intsignal, this.board.getCPU0().getMachineCycles() <= CYCLES_BETWEEN_INT);

        if (nextScreenRefresh <= System.currentTimeMillis()) {
          updateScreen();
          nextScreenRefresh = System.currentTimeMillis() + SCREEN_REFRESH_DELAY;
        }
      }
      finally {
        stepSemaphor.unlock();
      }
    }
  }

  private void updateScreen() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        board.getVideoController().refreshComponent();
      }
    });
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    scrollPanel = new javax.swing.JScrollPane();
    panelIndicators = new javax.swing.JPanel();
    menuBar = new javax.swing.JMenuBar();
    menuFile = new javax.swing.JMenu();
    menuFileReset = new javax.swing.JMenuItem();
    menuFileSelectDiskA = new javax.swing.JMenuItem();
    menuFileLoadSnapshot = new javax.swing.JMenuItem();
    menuOptions = new javax.swing.JMenu();
    menuOptionsShowIndicators = new javax.swing.JCheckBoxMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    addWindowFocusListener(new java.awt.event.WindowFocusListener() {
      public void windowGainedFocus(java.awt.event.WindowEvent evt) {
        formWindowGainedFocus(evt);
      }
      public void windowLostFocus(java.awt.event.WindowEvent evt) {
        formWindowLostFocus(evt);
      }
    });
    getContentPane().add(scrollPanel, java.awt.BorderLayout.CENTER);

    panelIndicators.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    panelIndicators.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
    getContentPane().add(panelIndicators, java.awt.BorderLayout.SOUTH);

    menuFile.setText("File");

    menuFileReset.setText("Reset");
    menuFileReset.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileResetActionPerformed(evt);
      }
    });
    menuFile.add(menuFileReset);

    menuFileSelectDiskA.setText("Select TRD for A");
    menuFileSelectDiskA.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileSelectDiskAActionPerformed(evt);
      }
    });
    menuFile.add(menuFileSelectDiskA);

    menuFileLoadSnapshot.setText("Load Snapshot");
    menuFileLoadSnapshot.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileLoadSnapshotActionPerformed(evt);
      }
    });
    menuFile.add(menuFileLoadSnapshot);

    menuBar.add(menuFile);

    menuOptions.setText("Options");

    menuOptionsShowIndicators.setSelected(true);
    menuOptionsShowIndicators.setText("Show indicator panel");
    menuOptionsShowIndicators.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsShowIndicatorsActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsShowIndicators);

    menuBar.add(menuOptions);

    setJMenuBar(menuBar);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void menuFileResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileResetActionPerformed
    this.board.reset();
  }//GEN-LAST:event_menuFileResetActionPerformed

  private void menuOptionsShowIndicatorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsShowIndicatorsActionPerformed
    this.panelIndicators.setVisible(this.menuOptionsShowIndicators.isSelected());
  }//GEN-LAST:event_menuOptionsShowIndicatorsActionPerformed

  private void menuFileSelectDiskAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileSelectDiskAActionPerformed
    final File selected = chooseFile("Select Disk A", null, null, new TRDFileFilter());
    if (selected != null) {
      try {
        final TRDOSDisk floppy = new TRDOSDisk(FileUtils.readFileToByteArray(selected), false);

        log.info("Loaded TRD disk " + floppy + " from file " + selected);

        this.board.getBetaDiskInterface().setDisk(floppy);
      }
      catch (IOException ex) {
        log.log(Level.WARNING, "Can't read TRD file [" + selected + ']', ex);
        JOptionPane.showMessageDialog(this, "Can't read TRD file", "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }//GEN-LAST:event_menuFileSelectDiskAActionPerformed

  private void formWindowLostFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowLostFocus
    this.board.getKeyboard().reset();
  }//GEN-LAST:event_formWindowLostFocus

  private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
    this.board.getKeyboard().reset();
  }//GEN-LAST:event_formWindowGainedFocus

  private void menuFileLoadSnapshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileLoadSnapshotActionPerformed
    stepSemaphor.lock();
    try{
    final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
    final File selected = chooseFile("Select snapshot", null, theFilter, new FormatZ80(), new FormatSNA());
    if (selected != null) {
      try {
        final Snapshot selectedFilter = (Snapshot)theFilter.get();
        selectedFilter.load(FileUtils.readFileToByteArray(selected));
        log.info("Loaded image from file " + selectedFilter.getName());
        stepSemaphor.lock();
        try {
          this.board.loadSnapshot(selectedFilter, false);
        }
        finally {
          stepSemaphor.unlock();
        }
      }
      catch (IOException ex) {
        log.log(Level.WARNING, "Can't read snapshot file [" + ex.getMessage() + ']', ex);
        JOptionPane.showMessageDialog(this, "Can't read snapshot file [" + ex.getMessage() + ']', "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
    }finally{
      stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuFileLoadSnapshotActionPerformed

  private File chooseFile(final String title, final File initial, final AtomicReference<FileFilter> selectedFilter, final FileFilter ... filter) {
    final JFileChooser chooser = new JFileChooser(initial);
    for(final FileFilter f : filter){
      chooser.addChoosableFileFilter(f);
    }
    chooser.setAcceptAllFileFilterUsed(false);
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle(title);
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    final File result;
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      result = chooser.getSelectedFile();
      if (selectedFilter!=null) {
        selectedFilter.set(chooser.getFileFilter());
      }
    }
    else {
      result = null;
    }
    return result;
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JMenuBar menuBar;
  private javax.swing.JMenu menuFile;
  private javax.swing.JMenuItem menuFileLoadSnapshot;
  private javax.swing.JMenuItem menuFileReset;
  private javax.swing.JMenuItem menuFileSelectDiskA;
  private javax.swing.JMenu menuOptions;
  private javax.swing.JCheckBoxMenuItem menuOptionsShowIndicators;
  private javax.swing.JPanel panelIndicators;
  private javax.swing.JScrollPane scrollPanel;
  // End of variables declaration//GEN-END:variables
}
