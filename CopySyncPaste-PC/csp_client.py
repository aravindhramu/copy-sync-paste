"""
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.


"""

import bluetooth
import pygtk
pygtk.require('2.0')
import gtk
from PyOBEX.client import *
import sys
import time
import os

DEVICE_NAME = "Aravindh phone"
FILE_NAME = "avi_clips.txt"
FOLDER_IN_DEVICE = "Aravindh"

def discover():
    devices = []
    try:
        devices = bluetooth.discover_devices()
    except:
        pass
    return devices

def findServicePort(devices,device_name,service_name):
    req_device = ""
    services = []
    try:
        for device in devices:
            if bluetooth.lookup_name(device) == device_name:
                services = bluetooth.find_service(address=device)
                req_device = device
        if req_device == "":
            raise BluetoothError
        if services == []:
            return req_device,None
        for service in services:
            if service['name'] == None or service == None:
                continue
            if 'name' in service and service['name'].find(service_name) != -1:
                if service['port'] != None:
                    return req_device,service['port']
        return req_device,None
    except Exception as e:
        return None,None

def getClipBoardContents():
    clipboard = gtk.clipboard_get()
    text = clipboard.wait_for_text()
    return text

def setClipBoardContents(text):
    clipboard = gtk.clipboard_get()
    clipboard.set_text(text)
    clipboard.store()


def main_client():
    print "Searching for devices.."
    devices = discover()
    if devices == []:
        print "Please turn on bluetooth in the devices around you."
        exit(1)
    device,port = findServicePort(devices,DEVICE_NAME,"OBEX")
    if device == None:
        print "Device \""+DEVICE_NAME+"\" doesn't have Bluetooth on"
        exit(1)
    if port == None:
        print "Required service not present in device.Cannot run application"
        exit(1)
    client = BrowserClient(device,port)
    print "Connecting.."
    try:
        client.connect()
    except Exception as e:
        print "Cannot establish connection.Aborting.."
    print "Connected.."
    contents = getClipBoardContents()
    client.setpath(FOLDER_IN_DEVICE)
    client.put(FILE_NAME,contents)
    device,port = findServicePort(devices,DEVICE_NAME,"btserver")
    if device == None:
        print "Device \""+DEVICE_NAME+"\" doesn't have Bluetooth on"
        exit(1)
    if port == None:
        print "Required service not present in device.Cannot run application"
        exit(1)
    try:
        client_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        client_sock.connect((device,port))
        print "Sending Clipboard contents..."
        client_sock.send(FOLDER_IN_DEVICE+"/"+FILE_NAME+'\n')
    except Exception as e:
        print "ERROR: Cannot establish connection !"+str(e)
        exit(1)
    #time.sleep(120)
    client_sock.close()
    client.disconnect()
    print "Done."


def main_server():
    server_sock=bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    #port = bluetooth.get_available_port(bluetooth.RFCOMM)
    server_sock.bind(("",0))
    server_sock.listen(1)
    port = server_sock.getsockname()[1]
    print "listening on port %d" % port
    uuid = "00001101-0000-1000-8000-00805F9B34FB"
    bluetooth.advertise_service( server_sock, "Copy-Sync-Paste", service_id=uuid )
    client_sock,address = server_sock.accept()
    print "Accepted connection from ",address
    clip_text = ""
    while True:
        try:
            text = client_sock.recv(1024)
            if len(text)!=0:
                clip_text=clip_text + text
            else:
                break
        except Exception as e:
            pass
            break
    print clip_text
    bluetooth.stop_advertising(server_sock)
    server_sock.close()
    client_sock.close()
    setClipBoardContents(clip_text)
    print getClipBoardContents()
    print "Done."




if __name__ == "__main__":
    if sys.argv[1] == '1':
        main_client()
    elif sys.argv[1] == '2':
        main_server()


