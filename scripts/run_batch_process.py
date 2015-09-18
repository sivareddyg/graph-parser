import sys
import os
import json
import tempfile

command = sys.argv[1]
def run_on_file(filePointer):
    fileName = filePointer.name
    filePointer.close()
    fullCommand = "%s < %s" %(command, fileName)
    sys.stderr.write(fullCommand + '\n');
    os.system(fullCommand)
    os.remove(fileName)

count = 0;
line = sys.stdin.readline()
batchSize = int(sys.argv[2])
filePointer = tempfile.NamedTemporaryFile(prefix="siva_batch_", dir="/tmp", delete=False);
for line in sys.stdin:
    sentence = json.loads(line)
    count += 1
    filePointer.write(line)
    if count % batchSize == 0:
        sys.stderr.write("Processing %d batch\n" %(count/batchSize))
        run_on_file(filePointer)
        sys.stderr.write("Processed %d lines\n" %(count))
        filePointer = tempfile.NamedTemporaryFile(prefix="siva_easyccg_", dir="/tmp", delete=False);

sys.stderr.write("Processing %d batch\n" %(count/batchSize + 1))
run_on_file(filePointer)
sys.stderr.write("Processed %d lines\n" %(count))
