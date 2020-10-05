-- example HTTP GET script which demonstrates setting the
-- HTTP method, body, and adding a header

counter = 0
request = function()
    path = "/v0/entity?id=key" .. counter
    wrk.method = "GET"
    wrk.body = nil
    counter = counter + 1
    return wrk.format(nil, path)
end