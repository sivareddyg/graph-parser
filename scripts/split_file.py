import sys

total_lines = int(sys.argv[1])
no_of_splits = int(sys.argv[2])
split_file_path = sys.argv[3]

split_size = total_lines/no_of_splits

count = 0
split_count = 0
split = open("%s.%d" %(split_file_path, split_count), "w")
for line in sys.stdin:
    count += 1
    split.write(line)
    if split_count + 1 < no_of_splits and count % split_size == 0:
        split_count += 1
        split.close()
        split = open("%s.%d" %(split_file_path, split_count), "w")
    if count > total_lines:
        break 

for line in sys.stdin:
    count += 1
    if count > total_lines:
        break 
    split.write(line)
split.close()
