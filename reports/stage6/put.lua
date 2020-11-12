-- example HTTP POST script which demonstrates setting the
-- HTTP method, body, and adding a header


key = 0;

request = function()
    path = "/v0/entity?id=" .. key
    wrk.method = "PUT"
    wrk.body = "{data: 123" .. key .. "}"
    key = key + 1
    return wrk.format(nil, path)
end
