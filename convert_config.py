import sys
count = 0
lines = []
new_lines = []
filename =  sys.argv[1]
new_filename = sys.argv[2]
f = open(filename)
for line in f:
    line = line.strip()
    if line == "":
        continue
    if line.startswith("#"):
        continue
    if count==0:
        count = int(line)
        new_lines.append(line)
    else:
        lines.append(line)
f.close()
need_convert = True
if need_convert:
    for i in range(len(lines)):
        if not lines[i].startswith("dc"):
            exit()
        fields = lines[i].split(" ")
        id = str(i+1)
        url = fields[0]+".utdallas.edu"
        port = fields[1]
        new_lines.append(id+" "+url+" "+port)
    for i in range(len(lines)):
        fields = lines[i].split(" ")
        id = str(i+1)
        new_lines.append(id+" "+" ".join(fields[2:]))

    with open(new_filename,"w") as f:
        for line in new_lines:
            f.write(line+"\n")
