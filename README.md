# KDE Lock / Unlock 




This project was built and tested on a Debian 13 system and I am using a Pixel watch 2.<br>
This project's goal is to use a google os watch to lock or unlock a PC via a direct bluetooth connection. It was originally intended to use KDE connect with a connection through an android device but there were too many difficulties. Instead it was decided that running a python listening server was much easier.



>[!Note]
**Under the Python folder is a python script that listens for the bluetooth connection from your watch. I created a system service for this to run at startup on my PC.**

>[!Important]
**In the WatchBluetoothConnection.kt on approx line 36 you need to hard code in the mac of the PC you are attempting to control** 
