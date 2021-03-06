/*
    Copyright 2019-2020 Dmitry Isaenko

    This file is part of NS-USBloader.

    NS-USBloader is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NS-USBloader is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NS-USBloader.  If not, see <https://www.gnu.org/licenses/>.
*/
package nsusbloader.COM.USB;

import javafx.concurrent.Task;
import nsusbloader.ModelControllers.LogPrinter;
import nsusbloader.NSLDataTypes.EFileStatus;
import nsusbloader.NSLDataTypes.EModule;
import nsusbloader.NSLDataTypes.EMsgType;
import org.usb4java.*;

import java.io.*;

import java.util.*;

// TODO: add filter option to show only NSP files
public class UsbCommunications extends Task<Void> {

    private LogPrinter logPrinter;
    private LinkedHashMap<String, File> nspMap;
    private String protocol;
    private boolean nspFilterForGl;

    public UsbCommunications(List<File> nspList, String protocol, boolean filterNspFilesOnlyForGl){
        this.protocol = protocol;
        this.nspFilterForGl = filterNspFilesOnlyForGl;
        this.nspMap = new LinkedHashMap<>();
        for (File f: nspList)
            nspMap.put(f.getName(), f);
        this.logPrinter = new LogPrinter(EModule.USB_NET_TRANSFERS);
    }

    @Override
    protected Void call() {
        logPrinter.print("\tStart", EMsgType.INFO);

        UsbConnect usbConnect = UsbConnect.connectHomebrewMode(logPrinter);

        if (! usbConnect.isConnected()){
            close(EFileStatus.FAILED);
            return null;
        }

        DeviceHandle handler = usbConnect.getNsHandler();

        TransferModule module;

        switch (protocol) {
            case "TinFoil":
                module = new TinFoil(handler, nspMap, this, logPrinter);
                break;
            case "GoldLeaf":
                module = new GoldLeaf(handler, nspMap, this, logPrinter, nspFilterForGl);
                break;
            case "GoldLeafv0.7.x":
                module = new GoldLeaf_07(handler, nspMap, this, logPrinter, nspFilterForGl);
                break;
            default:
                module = new GoldLeaf_05(handler, nspMap, this, logPrinter);
                break;
        }

        usbConnect.close();

        close(module.getStatus());

        return null;
    }

    /**
     * Report status and close
     */
    private void close(EFileStatus status){
        logPrinter.update(nspMap, status);
        logPrinter.print("\tEnd", EMsgType.INFO);
        logPrinter.close();
    }

}