--
-- Created by IntelliJ IDEA.
-- User: zvladn7
-- Date: 14.10.20
-- Time: 23:50
-- To change this template use File | Settings | File Templates.
--

condition = true

request = function()
    if (true) then
        math.randomseed(os.time())
        condition = false
    end
    path = "/v0/entity?id=key_" .. math.random(1, 2000);
    wrk.method = "GET"
    wrk.port = 8080
    return wrk.format(nil, path)
end
