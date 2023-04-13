insert into sectors (category, code, name, crckey, id) values 
('KIND', 'INDEX', '指数', concat(category,'.',code), crc32(crckey)), 
('KIND', 'STOCK', '股票', concat(category,'.',code), crc32(crckey)), 
('KIND', 'FUND', '基金', concat(category,'.',code), crc32(crckey)), 
('KIND', 'BOND', '债券', concat(category,'.',code), crc32(crckey)), 
('KIND', 'WARRANT', '权证', concat(category,'.',code), crc32(crckey)), 
('KIND', 'FUTURE', '期货', concat(category,'.',code), crc32(crckey)), 
('KIND', 'FOREX', '外汇', concat(category,'.',code), crc32(crckey)), 
('KIND', 'OPTION', '期权', concat(category,'.',code), crc32(crckey)), 
('KIND', 'TREASURY', '国债', concat(category,'.',code), crc32(crckey)), 
('KIND', 'ADDSHAOFFER', '增发', concat(category,'.',code), crc32(crckey)), 
('KIND', 'CONVBOND', '可转换债券', concat(category,'.',code), crc32(crckey)), 
('KIND', 'TREASREP', '国债回购', concat(category,'.',code), crc32(crckey));

insert into sectors (category, code, name, crckey, id) values 
('EXCHAN', 'N', '纽约', concat(category,'.',code), crc32(crckey)), 
('EXCHAN', 'L', '伦敦', concat(category,'.',code), crc32(crckey)), 
('EXCHAN', 'HK', '香港', concat(category,'.',code), crc32(crckey)), 
('EXCHAN', 'SS', '沪市', concat(category,'.',code), crc32(crckey)), 
('EXCHAN', 'SZ', '深市', concat(category,'.',code), crc32(crckey)),
('EXCHAN', 'OQ', '纳斯达克', concat(category,'.',code), crc32(crckey)); 

insert into sectors (category, code, name, crckey, id) values 
('BOARD', 'MAIN', '主板', concat(category,'.',code), crc32(crckey)), 
('BOARD', 'ASHARE', 'Ａ股', concat(category,'.',code), crc32(crckey)), 
('BOARD', 'BSHARE', 'Ｂ股', concat(category,'.',code), crc32(crckey)), 
('BOARD', 'HSHARE', 'Ｈ股', concat(category,'.',code), crc32(crckey)), 
('BOARD', 'SME', '中小', concat(category,'.',code), crc32(crckey)), 
('BOARD', 'GEM', '创业', concat(category,'.',code), crc32(crckey));

# EXCHANs
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('EXCHAN','.','SS')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS';

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('EXCHAN','.','SZ')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ';

# kind ---

# 沪市指数
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','INDEX')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and left(sec_infos.unisymbol, 3)='000';

# 沪市国债
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','TREASURY')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 3)='009' or left(sec_infos.unisymbol, 3)='010' or left(sec_infos.unisymbol, 3)='020');

# 沪市A股
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','STOCK')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 3)='600' or left(sec_infos.unisymbol, 3)='601');

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('BOARD','.','ASHARE')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 3)='600' or left(sec_infos.unisymbol, 3)='601');

# 沪市B股
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','STOCK')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 3)='900');

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('BOARD','.','BSHARE')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 3)='900');

# 沪市基金
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','FUND')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 3)='500' or left(sec_infos.unisymbol, 3)='510');

# 沪市权证
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','WARRANT')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 3)='580');

# 沪市可转换债券
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','CONVBOND')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 3)='100' or left(sec_infos.unisymbol, 3)='110' or left(sec_infos.unisymbol, 3)='112' or left(sec_infos.unisymbol, 3)='113');

# 沪市企业债券
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','BOND')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 3)='120' or left(sec_infos.unisymbol, 3)='129');

# 沪市债券
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','BOND')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SS' and (left(sec_infos.unisymbol, 1)='1' and left(sec_infos.unisymbol, 3)!='100' and left(sec_infos.unisymbol, 3)!='110' and left(sec_infos.unisymbol, 3)!='112' and left(sec_infos.unisymbol, 3)!='113' and left(sec_infos.unisymbol, 3)!='120' and left(sec_infos.unisymbol, 3)!='129');

# 深市A股
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','STOCK')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='00');

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('BOARD','.','ASHARE')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='00');

# 深市A股认股权证
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','WARRANT')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='03');

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('BOARD','.','ASHARE')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='03');

# 深市A股配股权证
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','WARRANT')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='08');

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('BOARD','.','ASHARE')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='08');

# 深市债券
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','BOND')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 3)='109' or left(sec_infos.unisymbol, 3)='111' or left(sec_infos.unisymbol, 3)='112' or left(sec_infos.unisymbol, 3)='115' or left(sec_infos.unisymbol, 2)='12');

# 深市国债
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','TREASURY')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='10' and left(sec_infos.unisymbol, 3)!='109' and left(sec_infos.unisymbol, 3)!='111' and left(sec_infos.unisymbol, 3)!='112' and left(sec_infos.unisymbol, 3)!='115');

# 深市国债回购
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','TREASREP')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='13');

# 深市基金
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','FUND')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='15' or left(sec_infos.unisymbol, 2)='16' or left(sec_infos.unisymbol, 2)='18');

# 深市B股
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','STOCK')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='20');

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('BOARD','.','BSHARE')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='20');

# 深市B股权证
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','WARRANT')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='28');

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('BOARD','.','BSHARE')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='28');

# 深市创业板
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','STOCK')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='30');

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('BOARD','.','GEM')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='30');

# 深市创业板配股权证
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','WARRANT')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='30');

insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('BOARD','.','GEM')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='30');

# 深市指数
insert into sector_secs (sectors_id, secs_id) 
select crc32(concat('KIND','.','INDEX')), secs_id from secs left join sec_infos on secs.secInfos_id=sec_infos.id 
where right(upper(sec_infos.unisymbol),3)='.SZ' and (left(sec_infos.unisymbol, 2)='39');