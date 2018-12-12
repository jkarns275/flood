data = {}
with open("test_results") as f:
    line = f.readline()
    while line:
        threshold, size = line.split(' ')
        threshold = int(threshold)
        size = int(size)
        if threshold not in data:
            data[threshold] = [0, 0]
        data[threshold][0] += size
        data[threshold][1] += 1
        line = f.readline()
keys = data.keys()
keys.sort()
for key in keys:
    data[key][0] /= data[key][1]
    print("{} -> {}".format(key, data[key][0]))
