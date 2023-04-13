# check secs that lacks sec_infos, regular reference:
select a.id, a.secInfos_id from secs as a left join sec_infos as b on a.secInfos_id=b.id where b.id is null;
# or,
select a.id, a.secInfos_id from secs as a where a.secInfos_id not in (select b.id from sec_infos as b);
# Try another side
select a.id, a.secInfos_id from secs as a left join sec_infos as b on a.id=b.secs_id where b.id is null;
# or
select a.id, a.secInfos_id from secs as a where a.id not in (select b.secs_id from sec_infos as b);

# Some of secs has secInfos_id = -1 ?
select a.secInfos_id from secs as a where a.secInfos_id=-1;
# Don't delete them, you should try to find or add corresponding sec_infos later

# check sec_infos that lacks secs
select a.id, a.secs_id, a.unisymbol from sec_infos as a left join secs on a.secs_id=secs.id where secs.id is null;
# or
select a.id, a.secs_id, a.unisymbol from sec_infos as a where a.secs_id not in (select secs.id from secs);
# you may want to delete them (double check here)
delete from sec_infos where sec_infos.secs_id not in (select secs.id from secs);

# check if all CRC32(uniSymbol) is great than existed max secs_id, if so, we are safe to do the convert:
select secs_id, crc32(upper(uniSymbol)) from sec_infos where crc32(upper(uniSymbol)) < (select max(secs_id) from sec_infos);

# create table secoldnew
create table secoldnew select secs_id as oldid, crc32(upper(unisymbol)) as newid, unisymbol as crckey from sec_infos;
create index secoldid_idx on secoldnew(oldid);
create index secnewid_idx on secoldnew(newid);

# ===== begin to update

# diable foreign key constain
set foreign_key_checks = 0;

# add column crckey to secs, sectors, exchanges
alter table secs add column crckey varchar(30) NOT NULL DEFAULT '' after id;
alter table sectors add column crckey varchar(30) NOT NULL DEFAULT '' after id;
alter table exchanges add column crckey varchar(30) NOT NULL DEFAULT '' after id;

# === secs id changed

# add column add adjOffset to sec_dividends
alter table sec_dividends add column adjOffset double NOT NULL DEFAULT 0 after adjWeight;

# update secs.id
update secs as a, secoldnew set a.id=secoldnew.newid, a.crckey=secoldnew.crckey where a.id=secoldnew.oldid;

# update tables that has secs_id
update sec_infos as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;
update sec_issues as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;
update sec_statuses as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;
update sec_dividends as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;
update sector_secs as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;
update companies as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;

# === sectors id changed

# create table sectoroldnew
create table sectoroldnew select id as oldid, crc32(upper(concat(category, '.', code))) as newid, concat(category, '.', code) as crckey from sectors;
# update sectors.id
update sectors as a, sectoroldnew set a.id=sectoroldnew.newid, a.crckey=sectoroldnew.crckey where a.id=sectoroldnew.oldid;
# update tables that has sectors_id
update sector_secs as a, sectoroldnew set a.sectors_id=sectoroldnew.newid where a.sectors_id=sectoroldnew.oldid;

# === exchanges id changed

# create table exchangeoldnew
create table exchangeoldnew select id as oldid, crc32(upper(code)) as newid, code as crckey from exchanges;
# update exchanges.id
update exchanges as a, exchangeoldnew set a.id=exchangeoldnew.newid, a.crckey=exchangeoldnew.crckey where a.id=exchangeoldnew.oldid;
# update tables that has sectors_id
update secs as a, exchangeoldnew set a.exchanges_id=exchangeoldnew.newid where a.exchanges_id=exchangeoldnew.oldid;


# === time cost update
# update tables that has secs_id
update tickers_last as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;
update quotes1d as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;
update quotes1m as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;

# === some result:
# mysql> update quotes1d as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;
# Query OK, 6626814 rows affected (18 min 31.77 sec)
# Rows matched: 6626814  Changed: 6626814  Warnings: 0
#
# mysql> update quotes1m as a, secoldnew set a.secs_id=secoldnew.newid where a.secs_id=secoldnew.oldid;
# Query OK, 181812747 rows affected (12 hours 3 min 28.82 sec)
# Rows matched: 181812747  Changed: 181812747  Warnings: 0

# try to clean secs and sec_infos
# Is there sec_infos that has empty unisymbol?
select * from sec_infos where unisymbol="" or unisymbol is null;
# Then the corresponding secs has crckey?
select a.id, secs.id, secs.crckey from sec_infos as a left join secs on a.secs_id=secs.id where unisymbol="" or unisymbol is null;

select a.secInfos_id, a.crckey from secs as a where a.secInfos_id=-1;
update sec_infos as b left join secs as a on b.secs_id=a.id set a.secInfos_id=b.id where a.secInfos_id=-1;