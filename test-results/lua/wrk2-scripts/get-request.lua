function RandomVariable(length)
  local res = ""
  for i = 1, length do
    res = res .. string.format("%02x", math.random(255))
  end
  return res
end

count = 0

request = function() 
  math.randomseed(os.time())
  path = "/v0/entity?id=key" .. count
  wrk.method = "GET"
  return wrk.format(nil, path)
end
