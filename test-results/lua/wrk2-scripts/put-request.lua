function RandomVariable(length)
  local res = ""
  for i = 1, length do
    res = res .. string.format('%02X',math.random(255))
  end
  return res
end

request = function() 
  math.randomseed(os.time())
  path = "/v0/entity?id=" .. RandomVariable(1024)
  wrk.method = "PUT"
  wrk.body = RandomVariable(1024)
  return wrk.format(nil, path)
end

