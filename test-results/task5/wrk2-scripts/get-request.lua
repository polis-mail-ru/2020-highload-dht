count = 0

request = function() 
  path = "/v0/entity?id=key" .. count .. "&replicas=2/3"
  wrk.method = "GET"
  return wrk.format(nil, path)
end
