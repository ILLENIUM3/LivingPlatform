-- 第一阶段基础设施探针，不包含用户业务表；在 live_platform 库执行。
CREATE TABLE IF NOT EXISTS platform_bootstrap (
    id INT NOT NULL PRIMARY KEY,
    probe_value VARCHAR(64) NOT NULL
);

-- 重复执行不覆盖已有值，方便通过修改探针值验证真实数据库读取。
INSERT INTO platform_bootstrap (id, probe_value)
SELECT 1, 'stage01-ready'
WHERE NOT EXISTS (SELECT 1 FROM platform_bootstrap WHERE id = 1);
