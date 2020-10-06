counter = 0

request = function()
    path = string.format("/v0/entities?start=%d&end=%d", counter, counter + 1)
    counter = math.fmod(counter + 1, 50000)
    counter = math.max(counter, 10000)
    return wrk.format(nil, path)
end
