count = 0

request = function() 
  math.randomseed(os.time())
  path = "/v0/entity?id=key" .. count .. "&replicas=2/3"
  wrk.method = "GET"
  return wrk.format(nil, path)
end
