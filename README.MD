# 功能描述
项目的目的是抽象出来Mybatis开发中的一些公共处理方案。

## fieldfilling

### 业务描述
自动给 creator、creator_name、create_time、modifier、modifier_name、modify_time、remark字段赋值。

### sql转换
#### insert

参考：cn.addenda.me.fieldfilling.sql.FieldFillingConvertorInsertTest

#### select

参考：cn.addenda.me.fieldfilling.sql.FieldFillingConvertorSelectTest

#### update

参考：cn.addenda.me.fieldfilling.sql.FieldFillingConvertorUpdateTest

#### delete

delete语句不需要添加字段。

### 使用

通过mybatis拦截器进行sql转换。参考：cn.addenda.me.fieldfilling.FieldFillingBootstrap。


### 填充的字段
```sql
alter table XXX
add column creator varchar(20) null;

alter table XXX
    add column creator_name varchar(20) null;

alter table XXX
    add column create_time bigint null;

alter table XXX
    add column modifier varchar(20) null;

alter table XXX
    add column modifier_name varchar(20) null;

alter table XXX
add column modify_time bigint null;

alter table XXX
add column remark varchar(50) null;
```


## idfilling

服务化的系统中，表都是需要有主键的，且不依赖数据库的主键生成策略（Oracle的序列，MySQL的自增主键）而是使用外部的ID生成算法。
ID填充就是将这些算法生成的ID填充到SQL语句中。

ID是填充到Entity里的，所以只有 Mapper#insert(TUser tUser); 这样的写法才能生效。

参考：cn.addenda.me.idfilling.IdFillingMybatisBootStrap


## 逻辑删除

### 业务描述

某些业务表的数据需要进行逻辑删除。将物理删除的sql转换为逻辑删除。

### sql转换
#### insert

参考：cn.addenda.me.logicaldeletion.sql.LogicalDeletionConvertorInsertTest

#### select

参考：cn.addenda.me.logicaldeletion.sql.LogicalDeletionConvertorSelectTest

#### update

参考：cn.addenda.me.logicaldeletion.sql.LogicalDeletionConvertorUpdateTest

#### delete

参考：cn.addenda.me.logicaldeletion.sql.LogicalDeletionConvertorDeleteTest


### 使用

通过mybatis拦截器进行sql转换。参考：cn.addenda.me.fieldfilling.FieldFillingBootstrap。


## mapresulthandler

系统的配置表，查询的时候，经常会需要 map 结构的数据，mapresulthandler的目的是方便从dao层的查询中获得一个map。
参考：cn.addenda.me.resulthandler.MapResultHandlerMybatisBootStrap

# 依赖
SQL解析器是个人开发的，运行此项目需要加入依赖。
地址：https://github.com/zhanjinhao/relational-operations-grammar。


# todo

1、增加窗口函数。
2、增加group_concat()语法。
3、增加 for update、lock in share mode语法。
4、toString() 增加分隔符为''的输出。