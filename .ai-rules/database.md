---
title: Database Schema Design
description: "VocaTa平台完整的PostgreSQL数据库设计方案，包含所有业务表和关联表。"
inclusion: always
---

# VocaTa PostgreSQL 数据库设计

## 设计规范概述

### 1. 企业级规范标准
- 表设计全面规范，字段定义清晰，包含必要业务字段和注释
- 确保高可维护性和扩展性
- 非必要情况下不创建索引，索引由后期手动添加

### 2. 关联模式
- 禁止使用主外键约束
- 所有表关系通过独立关联表实现多对多或一对多关系

### 3. 字段类型规范
- 禁止使用ENUM类型，统一使用SMALLINT表示枚举值
- 使用PostgreSQL标准数据类型
- 时间字段使用TIMESTAMP WITH TIME ZONE
- JSON数据使用JSONB类型

### 4. 命名规范
- 表名以`vocata_`开头
- 关联表以`_relation`结尾
- 字段名采用小写下划线命名法

### 5. 审计字段
每张表末尾包含标准审计字段：create_id、update_id、create_date、update_date、is_delete