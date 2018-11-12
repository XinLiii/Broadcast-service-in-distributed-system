Team member:
Bin Hu: bxh171130
Xin Li: xxl162230
Yang Song: yxs167230

1.to run the project:
change directory to project dir.
run command: bash launch.sh
after running, run command: bash cleanup.sh

2.check output:
output file of each node is generated in directory out/
example: out/dc02.utdallas.edu_5123

3.if need to use anothor config file, change the following line in the launch.ch and cleanup.sh:
CONFIGFILE=$PROJDIR/config3 ==> CONFIGFILE=$PROJDIR/<your config file>
