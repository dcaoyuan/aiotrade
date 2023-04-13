nohup ./deleteAfterDate.sh 20101111 10.8.0.139 faster faster &
20101111: date
10.8.0.139: host
faster:user
faster:password



# Note:
1. If you're running an operation on a large number of rows within a table that uses the InnoDB storage engine, you might see this error:

ERROR 1206 (HY000): The total number of locks exceeds the lock table size

MySQL is trying to tell you that it doesn't have enough room to store all of the row locks that it would need to execute your query. The only way to fix it for sure is to adjust innodb_buffer_pool_size and restart MySQL. By default, this is set to only 8MB, which is too small for anyone who is using InnoDB to do anything.

If you need a temporary workaround, reduce the amount of rows you're manipulating in one query. For example, if you need to delete a million rows from a table, try to delete the records in chunks of 50,000 or 100,000 rows. If you're inserting many rows, try to insert portions of the data at a single time.

Further reading:

MySQL Bug #15667 - The total number of locks exceeds the lock table size
MySQL Error 1206 » Mike R's Blog

2. If you forget to give the & at the end of line, and the process blocks the command input to the terminal window, you can put the process in the background "after the fact", by using Ctrl-Z. The process is suspended, and you get the command prompt back. The first thing you should do then is probably to give the command "bg", that resumes the process, but now in the background. 
