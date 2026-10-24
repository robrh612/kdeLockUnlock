# This is the KDE Unlock / Lock Readme
### The point of this project is to give us the ability to use my google os watch to lock or unlock my debian PC via a direct bluetooth connection. It was originally intended on working with KDE connect but I was running into too many difficulties, running my own listening server was much easier.

**Under the Python folder is a python server that listens for the bluetooth connection from your watch. I created a service for this to run at startup on my PC.**

**In the WatchBluetoothConnection.kt on approx line 36 you need to hard code in the mac of the PC you are attempting to control** 