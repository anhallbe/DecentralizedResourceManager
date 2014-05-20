from request_times import *
import numpy

times = get_time("asd.log")
print("asdasdasdasdasd")
for t in times:
	print(t)

print("Tasks: ", len(times))
print("Min: ", min(times))
print("Max: ", max(times))
print("Mean: ", numpy.mean(times))