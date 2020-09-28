counter = 0
request = function()
    path = "/v0/entity?id=key" .. counter
    wrk.method = "DELETE"
    wrk.body = nil
    counter = counter + 1
    return wrk.format(nil, path)
end