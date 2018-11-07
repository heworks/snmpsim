# snmpsim
The goal of this project is help people who needs a large number snmp devices for testing snmp polling. 
Currently only supports linux environment.

# Test
- Tested in Ubuntu with java 1.8 update 25. 

# Release 1.0.0
- grab the zip file under releases folder
- upload it to a linux machine.
- make sure you have java 1.8 update 25 installed on the machine.
- $unzip snmpsim-1.0.0.zip
- $mv application.properties.example application.properties
- $mv device.walk.example to device.walk
- modify application.properties to fit your need. 
- $sudo java -jar snmpsim-1.0.0.jar

# About application.properties
- interface=eth1
- snmpPort=16100
- deviceIPs=172.17.1.0/24,172.17.101.1/32
- dataDir=/root/dev/snmpsim
- deviceToSimulate=C3925.walk
- mibsToSimulate=SNMPv2-SMI.my,CISCO-SMI.my

"interface" should the name of the interface that you want the simulated to hide behind.
"snmpPort" is the port it will use to for snmp binding.
"deviceIPs" is a list of IPs that will be simulated. 
"dataDir" is path to where data is stored used
"deviceToSimulate" points to a walk file stored under {dataDir}/walkFiles
"mibsToSimulate" points to a comma separated list of mib files stored under {dataDir}/mibs

# Other
- walk files are files generated from snmpwalk command
- All devices will use oid values in "device.walk". They will look exactly the same, but each of them with 
  device serial string.
- walk file can be obtained using the following commands:
1) snmpwalk -t 10 -r 3 -v 2c -c public 10.1.150.11 -On .1.3.6.1.2 > C3925.walk
2) snmpwalk -t 10 -r 3 -v 2c -c public 10.1.150.11 -On .1.3.6.1.4 >> C3925.walk

- Sub interfaces are used to simulate the devices. Run "ip addr" command to see all the sub interfaces on Linux.
