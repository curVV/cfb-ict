### Install ###

```
git clone https://github.com/svenger87/cfb-ict.git

cd cfb-ict/src

# Add sources to compile to text file
find -name '*.java' > sources.txt

# Compile
javac @sources.txt

# Create ict.properties file
# See below

# Run
java -cp . cfb.ict.Ict ict.properties
```

#### Properties file ####  
```
cfb.ict.host = 192.168.1.1
cfb.ict.port = 14265
cfb.ict.neighbors = udp://neighbour1:14265;udp://neighbour2:14265
cfb.ict.neighborCooldownDuration = 86400
```
