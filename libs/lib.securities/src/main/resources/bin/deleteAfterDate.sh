#!/usr/bin/env bash

input=$1
host=$2
user=$3
password=$4

if [ $# -lt 4 ]
then
    echo "Need input parameter YYYYmmdd host user password"
    exit 1
fi

yyyy=${input:0:4}
mm=${input:4:2}
dd=${input:6:2}


timestamp="${yyyy}-${mm}-${dd} 00:00:00"
utimestamp="unix_timestamp('${timestamp}')"
utimestamp1000="${utimestamp}*1000"
posix=$RANDOM


#sql="select ${utimestamp};"
#sql2="select ${utimestamp1000};"
wheretime="time >= ${utimestamp1000}"

#cmd="/usr/local/mysql/bin/mysql --user=faster --password=faster --database=faster --host=${host} --execute=${sql}"

#mysql --user=faster --password=faster --database=faster --host=${host} --execute="${sql2}"

#------------------------delete quotes1d
sqldelquotes1d="delete  from quotes1d where ${wheretime};commit;"
echo "to execute: ${sqldelquotes1d}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqldelquotes1d}"
echo "delete quotes1d: success!"


#------------------------delete money_flows1d

sqldelmf1d="delete  from money_flows1d where ${wheretime};commit;"
echo "to execute: ${sqldelmf1d}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqldelmf1d}"
echo "delete money_flows1d: success!"

#------------------------delete tickers_last

sqldeltickerslast="delete  from tickers_last where ${wheretime};commit;"
echo "to execute: ${sqldeltickerslast}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqldeltickerslast}"
echo "delete tickers_last: success!"


#------------------------delete money_flows1m

sqldelmf1m="delete  from money_flows1m where secs_id between 1 and 5000000000 and ${wheretime};commit;"
echo "to execute: ${sqldelmf1m}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqldelmf1m}"
echo "delete money_flows1m: success!"

#------------------------delete quotes1m

sqldelquotes1m="delete  from quotes1m where secs_id between 1 and 5000000000 and ${wheretime};commit;"
echo "to execute: ${sqldelquotes1m}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqldelquotes1m}"
echo "delete quotes1m: success!"

#------------------------rename tickers;
sqlrentickers="drop table if exists tickers${posix}; alter table tickers rename to tickers${posix};"
echo "to execute: ${sqlrentickers}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqlrentickers}"

#DROP TABLE IF EXISTS `tickers`;

#------------------------rename executions;
sqlrenexecutions="drop table if exists executions${posix}; alter table executions rename to executions${posix};"
echo "to execute:${sqlrenexecutions}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqlrenexecutions}"

#------------------------re-create tickers & executions
echo "to re-create tickers & executions"
time mysql --user=${user} --password=${password} --database=faster --host=${host}<tickersexecutions.sql

