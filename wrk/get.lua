--
-- Created by IntelliJ IDEA.
-- User: zvladn7
-- Date: 26.09.20
-- Time: 20:56
-- To change this template use File | Settings | File Templates.
--

counter = 0
addToCounter = "_"

request = function()
    path = "/v0/entity?id=key" .. addToCounter .. counter
    wrk.method = "GET"
    wrk.port = 8080
    wrk.body = nil
    counter = counter + 1
    if counter > 32000 then
        counter = 0
        addToCounter = "_" .. addToCounter
    end
    return wrk.format(nil, path)
end
