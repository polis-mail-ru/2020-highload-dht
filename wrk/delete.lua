counter = 0

request = function()
    path = "/v0/entity?id=key" .. counter
    wrk.method = "DELETE"
    counter = counter + 1
    return wrk.format(nil, path)
end