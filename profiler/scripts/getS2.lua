cnt = 0
request = function ()
    path = "/v0/entity?replicas=1/3&id=key" .. cnt
    wrk.method = "GET"
    cnt = cnt + 1
    return wrk.format(nil, path)
end
