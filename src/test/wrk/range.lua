from, to = 1, 10

request = function()
    path = "/v0/entities?start=key" .. from .. "&end=key" .. to
    wrk.method = "GET"
    return wrk.format(nil, path)
end

