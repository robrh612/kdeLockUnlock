#!/usr/bin/env python3
import bluetooth
import subprocess
import sys

# Standard Serial Port Profile (SPP) UUID
SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

def main():
    print("Starting Debian Wayland Bluetooth Lock Server (PyBluez)...")

    server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    server_sock.bind(("", bluetooth.PORT_ANY))
    server_sock.listen(1)

    port = server_sock.getsockname()[1]

    try:
        bluetooth.advertise_service(
            server_sock,
            "KdeLockUnlockServer",
            service_id=SPP_UUID,
            service_classes=[SPP_UUID, bluetooth.SERIAL_PORT_CLASS],
            profiles=[bluetooth.SERIAL_PORT_PROFILE]
        )
    except Exception as e:
        print(f"Warning: Could not advertise service via SDP: {e}")

    print(f"Waiting for watch connection on RFCOMM channel {port}...")

    while True:
        try:
            client_sock, client_info = server_sock.accept()
            print(f"Accepted connection from watch: {client_info}")

            data = client_sock.recv(1024).decode('utf-8').strip()
            print(f"Received command: {data}")

            if data == "LOCK":
                result = subprocess.run(["loginctl", "lock-session"], capture_output=True, text=True)
                print(f"Lock result: {result.returncode}, {result.stderr}")
                client_sock.send(b"LOCKED\n")
            elif data == "UNLOCK":
                result = subprocess.run(["loginctl", "unlock-session"], capture_output=True, text=True)
                print(f"Unlock result: {result.returncode}, {result.stderr}")
                client_sock.send(b"UNLOCKED\n")
            else:
                print(f"Unknown command: {data}")
                client_sock.send(b"UNKNOWN\n")

            client_sock.close()
        except Exception as e:
            print(f"Connection error: {e}")

if __name__ == "__main__":
    main()
