#!/usr/bin/env bash

host=$1
user=$2
password=$3
sectors_filepath=$4
sector_secs_filepath=$5

if [ $# -lt 5 ]
then
    echo "Need input parameter host user password sectors_filepath sector_secs_filepath"
    exit 1
fi

#------------------------drop table sectors if exists
sqldropsectors="set foreign_key_checks = 0; drop table if exists sectors"
echo "to execute: ${sqldropsectors}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqldropsectors}"
echo "drop table sectors if exists: success!"

#------------------------rename money_flows1d , money_flows1m
sqlrenamemf="alter table money_flows1d rename to money_flows1d_old; alter table money_flows1m rename to money_flows1m_old; "
echo "to execute: ${sqlrenamemf}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqlrenamemf}"
echo "rename money_flows1d money_flows1m: success!"

#------------------------create table sectors ,sector_secs, money_flows1d, money_flows1m
echo "to create table sectors ,sector_secs, money_flows1d, money_flows1m"
time mysql --user=${user} --password=${password} --database=faster --host=${host}<createtable.sql
echo "create table success!"

#------------------------export and import sectors data
sqlexpsectors="select id, category, code, name from industries into outfile '${sectors_filepath}';"
echo "to execute: ${sqlexpsectors}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqlexpsectors}"
echo "export industries : success!"

sqlimpsectors="load data infile '${sectors_filepath}' into table sectors;"
echo "to execute: ${sqlimpsectors}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqlimpsectors}"
echo "import sectors : success!"


#------------------------export and import sector_secs data
sqlexpsector_secs="select company_industries.id, 0.0, industries_id, secs_id, 0, 0 from company_industries left join companies on companies.id=company_industries.companies_id into outfile '${sector_secs_filepath}';"
echo "to execute: ${sqlexpsector_secs}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqlexpsector_secs}"
echo "export company_industries : success!"

sqlimpsector_secs="load data infile '${sector_secs_filepath}' into table sector_secs;"
echo "to execute: ${sqlimpsector_secs}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqlimpsector_secs}"
echo "import sector_secs; : success!"

#------------------------ insert money_flow1d history  data
sqlinsmf1d="insert into money_flows1d(secs_id , time, totalVolume,totalAmount, totalVolumeIn,totalAmountIn, totalVolumeOut, totalAmountOut, totalVolumeEven, totalAmountEven,superVolume, superAmount,superVolumeIn,superAmountIn, superVolumeOut, superAmountOut, superVolumeEven, superAmountEven,largeVolume,largeAmount, largeVolumeIn,largeAmountIn, largeVolumeOut, largeAmountOut, largeVolumeEven, largeAmountEven,smallVolume, smallAmount,smallVolumeIn,smallAmountIn, smallVolumeOut, smallAmountOut, smallVolumeEven, smallAmountEven,flag) select secs_id,time, totalVolume,totalAmount,case when totalVolume>0 then totalVolume else 0 end totalVolumeIn, case when totalAmount>0 then totalAmount else 0 end totalAmountIn,case when totalVolume<0 then -1*totalVolume else 0 end totalVolumeOut, case when totalAmount<0 then -1*totalAmount else 0 end totalAmountOut,0,0,superVolume, superAmount,case when superVolume>0 then superVolume else 0 end superVolumeIn, case when superAmount>0 then superAmount else 0 end superAmountIn,case when superVolume<0 then -1*superVolume else 0 end superVolumeOut, case when superAmount<0 then -1*superAmount else 0 end superAmountOut,0,0,largeVolume,largeAmount,case when largeVolume>0 then largeVolume else 0 end largeVolumeIn, case when largeAmount>0 then largeAmount else 0 end largeAmountIn,case when largeVolume<0 then -1*largeVolume else 0 end largeVolumeOut, case when largeAmount<0 then -1*largeAmount else 0 end largeAmountOut,0,0,smallVolume, smallAmount,case when smallVolume>0 then smallVolume else 0 end smallVolumeIn, case when smallAmount>0 then smallAmount else 0 end smallAmountIn,case when smallVolume<0 then -1*smallVolume else 0 end smallVolumeOut, case when smallAmount<0 then -1*smallAmount else 0 end smallAmountOut,0,0,flag from money_flows1d_old; commit;"
echo "to execute: ${sqlinsmf1d}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqlinsmf1d}"
echo "insert into money_flow1d : success!"

#------------------------ insert money_flow1m history  data
sqlinsmf1m="insert into money_flows1m(secs_id , time, totalVolume,totalAmount, totalVolumeIn,totalAmountIn, totalVolumeOut, totalAmountOut, totalVolumeEven, totalAmountEven,superVolume, superAmount,superVolumeIn,superAmountIn, superVolumeOut, superAmountOut, superVolumeEven, superAmountEven,largeVolume,largeAmount, largeVolumeIn,largeAmountIn, largeVolumeOut, largeAmountOut, largeVolumeEven, largeAmountEven,smallVolume, smallAmount,smallVolumeIn,smallAmountIn, smallVolumeOut, smallAmountOut, smallVolumeEven, smallAmountEven,flag) select secs_id,time, totalVolume,totalAmount,case when totalVolume>0 then totalVolume else 0 end totalVolumeIn, case when totalAmount>0 then totalAmount else 0 end totalAmountIn,case when totalVolume<0 then -1*totalVolume else 0 end totalVolumeOut, case when totalAmount<0 then -1*totalAmount else 0 end totalAmountOut,0,0,superVolume, superAmount,case when superVolume>0 then superVolume else 0 end superVolumeIn, case when superAmount>0 then superAmount else 0 end superAmountIn,case when superVolume<0 then -1*superVolume else 0 end superVolumeOut, case when superAmount<0 then -1*superAmount else 0 end superAmountOut,0,0,largeVolume,largeAmount,case when largeVolume>0 then largeVolume else 0 end largeVolumeIn, case when largeAmount>0 then largeAmount else 0 end largeAmountIn,case when largeVolume<0 then -1*largeVolume else 0 end largeVolumeOut, case when largeAmount<0 then -1*largeAmount else 0 end largeAmountOut,0,0,smallVolume, smallAmount,case when smallVolume>0 then smallVolume else 0 end smallVolumeIn, case when smallAmount>0 then smallAmount else 0 end smallAmountIn,case when smallVolume<0 then -1*smallVolume else 0 end smallVolumeOut, case when smallAmount<0 then -1*smallAmount else 0 end smallAmountOut,0,0,flag from money_flows1m_old; commit;"
echo "to execute: ${sqlinsmf1m}"
time mysql --user=${user} --password=${password} --database=faster --host=${host} --execute="${sqlinsmf1m}"
echo "insert into money_flow1m : success!"