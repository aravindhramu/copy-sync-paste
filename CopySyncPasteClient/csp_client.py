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
    devices = bluetooth.discover_devices()
    return devices

def findServicePort(devices,device_name,service_name):
    req_device = ""
    services = []
    for device in devices:
        if bluetooth.lookup_name(device) == device_name:
            services = bluetooth.find_service(address=device)
            req_device = device
    if services == None:
	print "required service not found in mobile."
	exit(1)
    for service in services:
        if service['name'] == None:
            continue
        if 'name' in service and service['name'].find(service_name) != -1:
            if service['port'] != None:
                return req_device,service['port']

def getClipBoardContents():
    clipboard = gtk.clipboard_get()
    text = clipboard.wait_for_text()
    return text


if __name__ == "__main__":
    print "Searching for devices.."
    devices = discover()
    if devices == []:
        print "Please turn on bluetooth in the devices around you."
        exit(1)
    device,port = findServicePort(devices,DEVICE_NAME,"OBEX")
    if device == None:
        print "Please turn on bluetooth in the devices around you."
        exit(1)
    if port == None:
        print "Required service not present in device.Cannot run application"
        exit(1)
    client = BrowserClient(device,port)
    print "Connecting.."
    client.connect()
    print "Connected.."
    contents = getClipBoardContents()
    print "Sending Clipboard contents..."
    client.setpath(FOLDER_IN_DEVICE)
    client.put(FILE_NAME,contents)
    client.disconnect()
    device,port = findServicePort(devices,DEVICE_NAME,"btserver")
    if device == None:
        print "Please turn on bluetooth in the devices around you."
        exit(1)
    if port == None:
        print "Required service not present in device.Cannot run application"
        exit(1)
    try:
	    client_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
	    client_sock.connect((device,port))
	    client_sock.send(FOLDER_IN_DEVICE+"/"+FILE_NAME+'\n')
    except Exception as e:
            print "ERROR: Cannot establish connection !"+str(e)
            exit(1)
    #time.sleep(120)
    client_sock.close()
    print "Done."




