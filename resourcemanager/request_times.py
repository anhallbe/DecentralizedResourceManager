import os


def get_times():
    for file in os.listdir():
        if ".log" not in file:
            continue
        print("LOGFILE: {0}".format(file))
        get_time(file)


def get_time(filename):
    tasks_starts = dict()
    tasks_ends = dict()

    with open(filename, "r") as myfile:
        for line in myfile.readlines():
            line = line.replace("\n", "")
            values = line.split("\t")
            print(values)
            if values[1] == "start":
                tasks_starts[values[2]] = int(values[0])
            else:
                tasks_ends[values[2]] = int(values[0])

    results = list()
    print(tasks_ends, tasks_starts)
    for key in tasks_ends.keys():
        try:
            print(key)
            result = tasks_ends[key]-tasks_starts[key]
            print("TASK:{0} took:{1} ms".format(key, str(result)))
            results.append(result)
        except:
            print("error")
    return results

if __name__ == "__main__":
    #get_time("filnamn.log")
    get_times()