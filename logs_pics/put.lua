-- example HTTP POST script which demonstrates setting the
-- HTTP method, body, and adding a header

wrk.method = "PUT"
wrk.body = "123"
wrk.headers["Content-Type"] = "text/plain"
