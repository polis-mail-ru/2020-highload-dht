cnt = 0
request = function ()
    path = "/v0/entity?replicas=2/3&id=key" .. cnt
    wrk.method = "PUT"
    wrk.body = "value" .. cnt
    cnt = cnt + 1
    return wrk.format(nil, path)
end
