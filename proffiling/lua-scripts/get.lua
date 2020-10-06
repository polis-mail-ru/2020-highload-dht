counter = 0

function request()
	path = "/v0/entity?id=key" .. counter
	wrk.method = "GET"
	counter = counter + 1
	return wrk.format(nil, path)
end
