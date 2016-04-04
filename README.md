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
interface=eth1
snmpPort=16100
deviceIPs=172.17.1.0/24,172.17.101.1/32

"interface" should the name of the interface that you want the simulated to hide behind. 
"snmpPort" is the port it will use to for snmp binding.
"deviceIPs" is a list of IPs that will be simulated. 

# Other
- "device.walk" is a snmp walk file generated from snmpwalk command
- All devices will use oid values in "device.walk". They will look exactly the same, but each of them with 
  device serial string.
