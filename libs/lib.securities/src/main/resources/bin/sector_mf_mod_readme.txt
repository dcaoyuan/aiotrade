nohup ./sector_mf_mod.sh 10.8.0.138 faster faster /tmp/sectors /tmp/sector_secs >/tmp/aa 2>&1 &
10.8.0.138: host
faster:user
faster:password
/tmp/sectors: exported sector data temp file
/tmp/sector_sec: exported sector_secs data temp file 



# Note:
1.money_flow的历史数据导入除了原先的字段外，加入以下逻辑：
以totalVolume为例
如果totalVolume为正，则同时插入值totalVolumeIn
如果totalVolume为负，则同时插入值totalVolumeOut
insert into money_flows1m(secs_id , time, 
totalVolume,totalAmount, totalVolumeIn,totalAmountIn, totalVolumeOut, totalAmountOut, totalVolumeEven, totalAmountEven,
superVolume, superAmount,superVolumeIn,superAmountIn, superVolumeOut, superAmountOut, superVolumeEven, superAmountEven,
largeVolume,largeAmount, largeVolumeIn,largeAmountIn, largeVolumeOut, largeAmountOut, largeVolumeEven, largeAmountEven,
smallVolume, smallAmount,smallVolumeIn,smallAmountIn, smallVolumeOut, smallAmountOut, smallVolumeEven, smallAmountEven,
flag) 
select secs_id,time, 
totalVolume,totalAmount,case when totalVolume>0 then totalVolume else 0 end totalVolumeIn, case when totalAmount>0 then totalAmount else 0 end totalAmountIn,case when totalVolume<0 then -1*totalVolume else 0 end totalVolumeOut, case when totalAmount<0 then -1*totalAmount else 0 end totalAmountOut,0,0,
superVolume, superAmount,case when superVolume>0 then superVolume else 0 end superVolumeIn, case when superAmount>0 then superAmount else 0 end superAmountIn,case when superVolume<0 then -1*superVolume else 0 end superVolumeOut, case when superAmount<0 then -1*superAmount else 0 end superAmountOut,0,0,
largeVolume,largeAmount,case when largeVolume>0 then largeVolume else 0 end largeVolumeIn, case when largeAmount>0 then largeAmount else 0 end largeAmountIn,case when largeVolume<0 then -1*largeVolume else 0 end largeVolumeOut, case when largeAmount<0 then -1*largeAmount else 0 end largeAmountOut,0,0,
smallVolume, smallAmount,case when smallVolume>0 then smallVolume else 0 end smallVolumeIn, case when smallAmount>0 then smallAmount else 0 end smallAmountIn,case when smallVolume<0 then -1*smallVolume else 0 end smallVolumeOut, case when smallAmount<0 then -1*smallAmount else 0 end smallAmountOut,0,0,
flag 
from money_flows1m_old

2.money_flow历史数据导入在北京测试环境中两千多万条，大概耗时20分钟

3.导入后性能测试的简单脚本
select @@profiling;
set profiling =1;
execute select  statement: select * from money_flows1m where .....
show profiles;
show profile block io  for query n;   other type( ALL  | BLOCK IO  | CONTEXT SWITCHES  | CPU  | IPC  | MEMORY  | PAGE FAULTS  | SOURCE  | SWAPS)