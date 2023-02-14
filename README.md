Run
========
First, build the project in netbeans.

To run on Rover machine, execute following from command line,

java -jar /path/to/dist/RoboCam.jar --machine Rover --roverport 5002

To run on UI machine, execute following from command line,

java -jar /path/to/dist/RoboCam.jar --machine Rover --roverport 5002 --roverhost 127.0.0.1

Dependency:
============

sqlite-jdbc-3.23.1.jar

phidget22.jar

mysql-connector-java-5.0.8-bin


============

Network setup:
Rover-Port = 5001

use the first address when in the barn with the control center computer (aka the ui computer).

This address can be found by running the 'MyFirstInternetAddress.java' file on the rover computer.

Enter this address on both computers within the robo-config.ini file as the 'Rover-Host' value.

Rover-Host = 173.121.11.74 -- look back at prior versions and branches for more specifics
use the second address when in the house with the ui computer:
also will need to set up the port forwarding in unifi.ubnt.com software for each camera and the rover computer.
calix router in barn   --378.62.821.70 -- the ui machine needs to hit the public IP of the rover network (as detected from the rover computer).
also, the router on the rover network needs to have port forwarding set to forward the 5001 port.
house -> internet -> barn router (port forward the switch and port 5001) -> switch (port forward 5001) -> rover
allow 5001 through firewall at all points.

enter the rover address into the house computer and enter the house address into the rover computer.

x_Rover-Host = 378.62.821.70 -- look back at prior versions and branches for more specifics

ComputerNetworkLocation = ExternalNetwork
x_ComputerNetworkLocation = LocalNetwork
