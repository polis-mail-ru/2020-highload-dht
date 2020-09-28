local counter = 1

function response()
   if counter == 100 then
      wrk.thread:stop()
   end
   counter = counter + 1
end