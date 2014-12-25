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
package com.igormaznitsa.zxpoly.components.betadisk;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class VG93 {

  // Данный флаг меняется с каждым чтением регистра статуса. Он был введен для иммитации изменений состояния регистра.
  private boolean lg_Ready;

  // Время в миллисекундах, в течение которого операция чтение-записи валидна, если оно превышено, то будет выдан флаг "потеря данных"
  private final static long TIMEOUT = 2500;

  /**
   * Флаг показывающий что сигнал RESET активен и контроллер не может работать
   */
  public boolean lg_Reset;

  public static final int COMMAND_STATUS_NONE = 0;
  public static final int COMMAND_STATUS_TYPE1 = 1;
  public static final int COMMAND_STATUS_TYPE2 = 2;
  public static final int COMMAND_STATUS_TYPE3 = 3;
  public static final int COMMAND_STATUS_TYPE4 = 4;

  /**
   * Статус последней команды
   */
  public int i_CurrentCommandStatus;

  /**
   * Флаг, показывающий в какую сторону двигать головку.. true - в сторону
   * величения номера, false - к нулевой
   */
  public boolean lg_HeadStepIncrease;

  public static final int REG_COMMAND = 0x00;
  public static final int REG_STATUS = 0x01;
  public static final int REG_TRACK = 0x02;
  public static final int REG_SECTOR = 0x03;
  public static final int REG_DATA = 0x04;
  
  private final int [] Registers = new int [5]; 
  
  /**
   * Указатель позиции текущей операции чтения/записи в массиве данных диска
   */
  public int i_OperationPointer;

  /**
   * Количство байт для чтения/записи текущей командой
   */
  public int i_BytesToOperate;

  /**
   * Счетчик обработанных байт в текущем секторе, используется для увеличения
   * номера сектора
   */
  public int i_SectorBytesCounter;

  /**
   * Присвоить сигнал RESET, сброс происходит при переходе из true в false
   *
   * @param _flag состояние сигнала reset (true активен false неактивен)
   */
  public final void setResetSignal(boolean _flag) {
    if (lg_Reset && !_flag) {
      reset();
    }
    lg_Reset = _flag;
  }

  /**
   * The Current disk.
   */
  private final AtomicReference<Floppy> currentDisk = new AtomicReference<>();

  public VG93() {
    reset();
  }

  /**
   * Инициализация контроллера
   */
  public final void reset() {
    Arrays.fill(Registers, 0);
    i_BytesToOperate = 0;
    setStatusForAux(false);
  }

  public final void setDisk(final Floppy disk) {
    this.currentDisk.set(disk);
  }

  private void setStatusRegBit(int _bit) {
    Registers[REG_STATUS] |= (1 << _bit);
  }

  private void resetStatusRegBit(int _bit) {
    Registers[REG_STATUS] &= ~(1 << _bit);
  }

  private final void setStatusForAux(boolean _headLoaded) {
    final Floppy currentDisk = this.currentDisk.get();
    
    if (currentDisk == null) {
      setStatusRegBit(7);
    }
    else {
      resetStatusRegBit(7);
    }

    if (currentDisk != null && currentDisk.isWriteProtect()) {
      setStatusRegBit(6);
    }
    else {
      resetStatusRegBit(6);
    }

    if (_headLoaded) {
      setStatusRegBit(5);
    }
    else {
      resetStatusRegBit(5);
    }

    resetStatusRegBit(4);
    resetStatusRegBit(3);

    if (Registers[REG_TRACK] == 0) {
      setStatusRegBit(2);
    }
    else {
      resetStatusRegBit(2);
    }

    resetStatusRegBit(0);
  }

  /**
   * Установить регистр статуса для команд чтения
   */
  private final void setStatusForReadOperations() {
    final Floppy currentDisk = this.currentDisk.get();
    
    if (currentDisk == null) {
      i_BytesToOperate = 0;
      Registers[REG_STATUS] = 0x80;
      return;
    }
    resetStatusRegBit(7);

    resetStatusRegBit(6);
    resetStatusRegBit(5);

    if ((i_BytesToOperate > 0 && i_OperationPointer >= currentDisk.size()) || Registers[REG_SECTOR] == 0 || Registers[REG_SECTOR] > 16) {
      i_BytesToOperate = 0;
      setStatusRegBit(4);
    }
    else {
      resetStatusRegBit(4);
    }

    resetStatusRegBit(3);
    resetStatusRegBit(2);
    setStatusRegBit(1);

    if (i_BytesToOperate > 0) {
      setStatusRegBit(0);
    }
    else {
      resetStatusRegBit(0);
    }
  }

  /**
   * Установить регистр статуса для команд записи
   */
  private final void setStatusForWriteOperations() {
    final Floppy currentDisk = this.currentDisk.get();
    
    if (currentDisk == null) {
      i_BytesToOperate = 0;
      Registers[REG_STATUS] = 0x80;
      return;
    }

    resetStatusRegBit(7);

    if (currentDisk.isWriteProtect()) {
      setStatusRegBit(6);
      setStatusRegBit(5);
    }
    else {
      resetStatusRegBit(6);
      resetStatusRegBit(5);
    }

    if ((i_BytesToOperate > 0 && i_OperationPointer >= currentDisk.size()) || Registers[REG_SECTOR] == 0 || Registers[REG_SECTOR] > 16) {
      setStatusRegBit(4);
      i_BytesToOperate = 0;
    }
    else {
      resetStatusRegBit(4);
    }

    resetStatusRegBit(3);

    resetStatusRegBit(2);
    setStatusRegBit(1);
    if (i_BytesToOperate > 0) {
      setStatusRegBit(0);
    }
    else {
      resetStatusRegBit(0);
    }
  }

  /**
   * Расчитать смещение данных в массиве
   *
   * @param _side сторона диска 0,1
   * @param _track дорожка диска
   * @param _sector сектор (1-16)
   * @return смещение в массиве байт
   */
  private static int _calculatePointer(int _side, int _track, int _sector) {
    int i_result = (_track * 2 + _side) * 256 * 16 + (_sector - 1) * 256;
    return i_result;
  }

  /**
   * Запись данных в регистр данных и в массив данных при соответствующей
   * операции
   *
   * @param _data данные к записи
   */
  public final void setDataReg(int _data) {
    final Floppy currentDisk = this.currentDisk.get();
    
    Registers[REG_DATA] = _data;

    if (i_BytesToOperate == 0) {
      return;
    }

    if (currentDisk != null && !currentDisk.isWriteProtect()) {
      switch ((Registers[REG_COMMAND] >>> 4) & 0xF) {
        case 0xF: // запись дорожки
        {
          if (currentDisk != null && i_OperationPointer < currentDisk.size() && !currentDisk.isWriteProtect()) {
            currentDisk.write(i_OperationPointer,_data);
          }

          i_BytesToOperate--;
          i_OperationPointer++;
          i_SectorBytesCounter++;

          if (i_SectorBytesCounter == 256) {
            i_SectorBytesCounter = 0;
            Registers[REG_SECTOR]++;
            if (Registers[REG_SECTOR] > 16) {
              i_BytesToOperate = 0;
              Registers[REG_SECTOR] = 16;
            }
          }

          setStatusForWriteOperations();
        }
        break;
        case 0xA: // запись сектора
        {
          if (currentDisk != null && i_OperationPointer < currentDisk.size() && !currentDisk.isWriteProtect()) {
            currentDisk.write(i_OperationPointer,_data);
          }
          i_OperationPointer++;
          i_SectorBytesCounter++;
          i_BytesToOperate--;

          if (i_SectorBytesCounter == 256) {

            i_SectorBytesCounter = 0;

            Registers[REG_SECTOR]++;
            if (Registers[REG_SECTOR] > 16) {
              Registers[REG_SECTOR] = 1;
            }
          }

          setStatusForWriteOperations();
        }
        break;
        case 0xB: // запись секторов
        {
          if (currentDisk != null && i_OperationPointer < currentDisk.size() && !currentDisk.isWriteProtect()) {
            currentDisk.write(i_OperationPointer, _data);
          }

          i_BytesToOperate--;
          i_OperationPointer++;
          i_SectorBytesCounter++;

          if (i_SectorBytesCounter == 256) {
            i_SectorBytesCounter = 0;
            Registers[REG_SECTOR]++;
            if (Registers[REG_SECTOR] > 16) {
              i_BytesToOperate = 0;
              Registers[REG_SECTOR] = 16;
            }
          }

          setStatusForWriteOperations();
        }
        break;
        default:
          throw new RuntimeException("Unsupported write command");
      }
    }
    setStatusForWriteOperations();
  }

  /**
   * Проверка на активность выполнения текущей операции и контроллера в целом
   *
   * @return true если активен и false если не активен
   */
  public final boolean isActiveOperation() {
    return (!lg_Reset
            && ((i_CurrentCommandStatus == VG93.COMMAND_STATUS_TYPE2
            || i_CurrentCommandStatus == VG93.COMMAND_STATUS_TYPE3)
            && i_BytesToOperate > 0)
            );
  }

  /**
   * Чтение регистра данных или массива данных (в зависимости от команды)
   *
   * @return регистр данных
   */
  public final int getDataReg() {
    final Floppy currentDisk = this.currentDisk.get();
    
    if (i_BytesToOperate == 0) {
      return Registers[REG_DATA];
    }
    int i_result = Registers[REG_DATA];

    switch ((Registers[REG_COMMAND] >>> 4) & 0xF) {
      case 0xC: {
                //address reading
        //System.out.println("Read address" + i_Reg_Track + " sector " + i_Reg_Sector);

        i_BytesToOperate--;
        switch (i_BytesToOperate) {
          case 5:
            i_result = currentDisk.getCurrentTrackIndex();
            break;
          case 4:
            i_result = 0;
            break;
          case 3:
            i_result = Registers[REG_SECTOR];
            break;
          case 2:
            i_result = 1;
            break;
          case 1:
            i_result = 0;
            break;
          case 0:
            i_result = 0;
            break;
          default: {
            throw new RuntimeException("Wrong index");
          }
        }

        setStatusForReadOperations();
      }
      break;

      case 0xE:// track reading
      {
        if (currentDisk != null && i_OperationPointer < currentDisk.size()) {
          i_result = currentDisk.read(i_OperationPointer);
        }

        i_SectorBytesCounter++;
        i_BytesToOperate--;

        if (i_SectorBytesCounter == 256) {
          Registers[REG_SECTOR]++;
          i_SectorBytesCounter = 0;

          if (Registers[REG_SECTOR] > 16) {
            Registers[REG_SECTOR] = 1;
            i_BytesToOperate = 0;
          }
        }
        setStatusForReadOperations();
      }
      break;

      case 0x8: // one sector reading
      {
        //System.out.println("Read track " + i_Reg_Track + " sector " + i_Reg_Sector + " pntr " + i_OperationPointer);

        if (currentDisk != null && i_OperationPointer < currentDisk.size()) {
          i_result = currentDisk.read(i_OperationPointer);
        }

        i_BytesToOperate--;
        i_OperationPointer++;
        i_SectorBytesCounter++;
        if (i_SectorBytesCounter == 256) {
          i_BytesToOperate = 0;
          Registers[REG_SECTOR]++;

          if (Registers[REG_SECTOR] > 16) {
            Registers[REG_SECTOR] = 1;
          }
        }

        setStatusForReadOperations();
      }
      break;
      case 0x9: // multiple sectors reading
      {
        //System.out.println("Read trackS " + i_Reg_Track + " sector " + i_Reg_Sector + " pntr " + i_OperationPointer);

        if (currentDisk != null && i_OperationPointer < currentDisk.size()) {
          i_result = currentDisk.read(i_OperationPointer);
        }

        i_BytesToOperate--;
        i_OperationPointer++;
        i_SectorBytesCounter++;

        if (i_SectorBytesCounter == 256) {
          i_SectorBytesCounter = 0;
          Registers[REG_SECTOR]++;

          if (Registers[REG_SECTOR] > 16) {
            i_BytesToOperate = 0;
            Registers[REG_SECTOR] = 1;
          }
        }

        setStatusForReadOperations();
      }
      break;
    }

    return i_result;
  }

  /**
   * Задать команду к выполнению контроллером
   *
   * @param _command код команды
   */
  public final void setCommandReg(int _command) {
    final Floppy currentDisk = this.currentDisk.get();
    
    Registers[REG_COMMAND] = _command;

    int i_side = _command & 0b1000;

    switch ((_command >>> 4) & 0xF) {
      case 0x0: // восстановление
      {
        //System.out.println("VG Восстановление");
        i_CurrentCommandStatus = COMMAND_STATUS_TYPE1;
        Registers[REG_TRACK] = 0;
        setStatusForAux((_command & 8) == 0);
      }
      break;
      case 0x1: // поиск
      {
        //System.out.println("VG Поиск "+i_Reg_Data);
        i_CurrentCommandStatus = COMMAND_STATUS_TYPE1;

        if (Registers[REG_TRACK] > Registers[REG_DATA]) {
          lg_HeadStepIncrease = true;
        }
        else {
          if (Registers[REG_TRACK] <= Registers[REG_DATA]) {
            lg_HeadStepIncrease = false;
          }
        }

        Registers[REG_TRACK] = Registers[REG_DATA];

        if (currentDisk != null) {
          currentDisk.setCurrentTrackIndex(Registers[REG_TRACK]);
        }

        setStatusForAux((_command & 8) == 0);
      }
      break;
      case 0x2: // шаг
      case 0x3: {
        //System.out.println("VG Шаг ");

        i_CurrentCommandStatus = COMMAND_STATUS_TYPE1;

        if (lg_HeadStepIncrease) {
          if (Registers[REG_TRACK] < 255) {
            Registers[REG_TRACK]++;
          }
          else {
            if (Registers[REG_TRACK] > 0) {
              Registers[REG_TRACK]--;
            }
          }
        }

        if (currentDisk != null) {
          currentDisk.setCurrentTrackIndex(Registers[REG_TRACK]);
        }

        setStatusForAux((_command & 8) == 0);
      }
      break;
      case 0x4: // шаг вперед
      case 0x5: {
        //System.out.println("VG Шаг вперед");

        i_CurrentCommandStatus = COMMAND_STATUS_TYPE1;

        lg_HeadStepIncrease = true;
        if (Registers[REG_TRACK] < 255) {
          Registers[REG_TRACK]++;
        }

        if (currentDisk != null) {
          currentDisk.setCurrentTrackIndex(Registers[REG_TRACK]);
        }

        i_CurrentCommandStatus = COMMAND_STATUS_TYPE1;

        setStatusForAux((_command & 8) == 0);
      }
      break;
      case 0x6: // шаг назад
      case 0x7: {
        //System.out.println("VG Шаг назад");

        i_CurrentCommandStatus = COMMAND_STATUS_TYPE1;

        lg_HeadStepIncrease = false;
        if (Registers[REG_TRACK] > 0) {
          Registers[REG_TRACK]--;
        }

        if (currentDisk != null) {
          currentDisk.setCurrentTrackIndex(Registers[REG_TRACK]);
        }

        i_CurrentCommandStatus = COMMAND_STATUS_TYPE1;

        setStatusForAux((_command & 8) == 0);
      }
      break;
      case 0x8: // чтение сектора
      {
        //System.out.println("VG Чтение сектора "+i_Reg_Track+':'+i_Reg_Sector);

        i_CurrentCommandStatus = COMMAND_STATUS_TYPE2;

        i_OperationPointer = _calculatePointer(i_side, Registers[REG_TRACK], Registers[REG_SECTOR]);
        i_BytesToOperate = 256;
        i_SectorBytesCounter = 0;

        setStatusForReadOperations();
      }
      break;
      case 0x9: // чтение секторов
      {
        //System.out.println("VG Чтение секторов "+i_Reg_Track+':'+i_Reg_Sector);

        i_CurrentCommandStatus = COMMAND_STATUS_TYPE2;

        i_OperationPointer = _calculatePointer(i_side, Registers[REG_TRACK], Registers[REG_SECTOR]);
        i_BytesToOperate = 16 * 256;
        i_SectorBytesCounter = 0;

        setStatusForReadOperations();
      }
      break;
      case 0xA:// запись сектора
      {
        i_CurrentCommandStatus = COMMAND_STATUS_TYPE2;

        i_OperationPointer = _calculatePointer(i_side, Registers[REG_TRACK], Registers[REG_SECTOR]);
        i_BytesToOperate = 256;
        i_SectorBytesCounter = 0;

        setStatusForWriteOperations();
      }
      break;
      case 0xB:// запись секторов
      {
        i_CurrentCommandStatus = COMMAND_STATUS_TYPE2;

        i_OperationPointer = _calculatePointer(i_side, Registers[REG_TRACK], Registers[REG_SECTOR]);
        i_BytesToOperate = 16 * 256;
        i_SectorBytesCounter = 0;

        setStatusForWriteOperations();
      }
      break;
      case 0xC: // чтение адреса
      {
        //System.out.println("VG Чтение адреса");
        Registers[REG_SECTOR]++;
        if (Registers[REG_SECTOR] > 16) {
          Registers[REG_SECTOR] = 1;
        }
        
        i_CurrentCommandStatus = COMMAND_STATUS_TYPE3;

        i_BytesToOperate = 6;

        setStatusForReadOperations();
      }
      break;
      case 0xE: // чтение дорожки
      {
        //System.out.println("VG Чтение дорожки "+i_Reg_Track);
        i_CurrentCommandStatus = COMMAND_STATUS_TYPE3;

        Registers[REG_SECTOR] = 1;

        i_OperationPointer = _calculatePointer(i_side, Registers[REG_TRACK], 1);
        i_BytesToOperate = 256 * 16;

        setStatusForReadOperations();
      }
      break;
      case 0xF: // запись дорожки
      {
        i_CurrentCommandStatus = COMMAND_STATUS_TYPE3;

        Registers[REG_SECTOR] = 1;

        i_OperationPointer = _calculatePointer(i_side, Registers[REG_TRACK], 1);
        i_BytesToOperate = 256 * 16;

        setStatusForWriteOperations();
      }
      break;
      case 0xD: // принудительное прерывание
      {
        i_CurrentCommandStatus = COMMAND_STATUS_TYPE4;
        i_BytesToOperate = 0;

        if ((Registers[REG_STATUS] & 1) == 0) {
          // выставляем состояние как у вспомогательных команд
          setStatusForAux(true);
        }
        else {
          // так как прервана команда, то сбрасываем бит работы
          Registers[REG_STATUS] &= ~1;
        }
      }
      break;
    }
  }

  /**
   * Записать значение в регистр дорожки
   *
   * @param _track новое значение
   */
  public final void setTrackReg(int _track) {
    Registers[REG_TRACK] = _track & 0xFF;
  }

  /**
   * Получить значение регистра дорожки
   *
   * @return значение регистра
   */
  public final int getTrackReg() {
    return Registers[REG_TRACK];
  }

  /**
   * Записать значение в регистр сектора
   *
   * @param _sect новое значение регистра
   */
  public final void setSectorReg(int _sect) {
    Registers[REG_SECTOR] = _sect & 0xFF;
  }

  /**
   * Получить значение регистра сектора
   *
   * @return значение регистра сектора
   */
  public final int getSectorReg() {
    return Registers[REG_SECTOR];
  }

  /**
   * Получить значение регистра статуса
   *
   * @return значение регистра статуса
   */
  public final int getStatusReg() {
    final Floppy currentDisk = this.currentDisk.get();
    
    lg_Ready = !lg_Ready;
    
    if (lg_Reset) {
      Registers[REG_STATUS] = 0;
      if (currentDisk != null) {
        setStatusRegBit(7);
      }
      return Registers[REG_STATUS];
    }

    switch (i_CurrentCommandStatus) {
      case COMMAND_STATUS_NONE: {
        if (currentDisk != null) {
          // возвращаем просто готовность привода
          return 0x80;
        }
      }
      break;
      case COMMAND_STATUS_TYPE1: {
        if (currentDisk != null) {
          // иммитируем индексный импульс
          if (lg_Ready) {
            setStatusRegBit(1);
          }
          else {
            resetStatusRegBit(1);
          }
        }
        else {
          resetStatusRegBit(1);
        }
      }
      break;
      case COMMAND_STATUS_TYPE3:
      case COMMAND_STATUS_TYPE2: {
          // иммитируем смену флага "готовность данных"
          if (lg_Ready) {
            setStatusRegBit(1);
          }
          else {
            resetStatusRegBit(1);
          }
      }
      break;
    }
    return Registers[REG_STATUS];
  }
}
