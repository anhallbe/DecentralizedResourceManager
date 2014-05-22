from pylab import *
from request_times import *
import numpy as np
import scipy.stats as stats

times = get_time("asd.log")
times = sort(times)
times = times[0:len(times)-len(times)/100]

t = range(len(times))
s = times
plot(t, s)

xlabel('request n')
ylabel('time (ms)')
str = "Median: ", int(np.average(times))
title(str)
grid(True)
savefig("test.png")
show()