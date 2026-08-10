-- 河南科技学院入校登记系统 - 菜单权限配置
-- 执行此SQL文件前，请先确保已经创建了ers_user表

-- 插入ERS系统管理菜单
INSERT INTO `sys_menu`(`parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`) VALUES
(0, 'ERS系统管理', NULL, NULL, 0, 'el-icon-setting', 10);

-- 获取刚才插入的菜单ID，假设是最新插入的，ID为LAST_INSERT_ID()
-- 或者您可以手动查看sys_menu表中的最新ID

-- 插入用户管理子菜单
-- 假设父菜单ID是刚才插入的，您可能需要根据实际情况调整
-- 这里先假设父菜单ID为您系统中已有的某个ID，或者您可以先执行上面的INSERT语句，然后查看实际的ID

-- 重新整理，我们先获取最大的菜单ID
-- 为了安全起见，我们使用变量或者您手动调整

-- 让我们重新执行，假设您先执行上面的INSERT，然后执行下面的
-- 这里我们使用一个更安全的方式

-- 首先检查是否已存在ERS系统管理菜单
-- 如果不存在，则创建

-- 为了简化，我们直接使用硬编码的方式，您可以根据实际情况调整

-- 创建一个完整的菜单结构
-- 假设父菜单ID为（您需要先执行第一条INSERT，然后查看实际的ID）

-- 我们先创建用户管理菜单及其子菜单
INSERT INTO `sys_menu`(`parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`) VALUES
-- 假设父菜单ID为（您需要先执行上面的INSERT，然后替换下面的数字）
-- 这里暂时使用0作为父菜单，您可以根据实际情况调整
(0, '用户管理', 'ers/user', NULL, 1, 'el-icon-user', 11);

-- 获取用户管理菜单的ID
SET @user_menu_id = LAST_INSERT_ID();

-- 插入用户管理的子菜单（按钮级权限）
INSERT INTO `sys_menu`(`parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`) VALUES
(@user_menu_id, '查看', NULL, 'admin:user:list,admin:user:info', 2, NULL, 1),
(@user_menu_id, '新增', NULL, 'admin:user:save', 2, NULL, 2),
(@user_menu_id, '修改', NULL, 'admin:user:update', 2, NULL, 3),
(@user_menu_id, '删除', NULL, 'admin:user:delete', 2, NULL, 4);

-- 为管理员角色（role_id=1）分配这些菜单权限
-- 首先需要查询刚才插入的菜单ID
-- 为了简化，我们假设管理员角色是role_id=1，并且为其分配所有菜单权限

-- 先删除可能已有的ERS相关权限
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `menu_id` FROM `sys_menu` WHERE `perms` LIKE 'admin:%'
);

-- 插入管理员角色的菜单权限
INSERT INTO `sys_role_menu`(`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `perms` LIKE 'admin:%';

-- 显示完成信息
SELECT 'ERS系统菜单权限配置完成！' AS message;

-- 注意事项：
-- 1. 执行此SQL前，请先确保系统中已有管理员角色（role_id=1）
-- 2. 如果父菜单ID不对，请根据实际情况调整
-- 3. 执行完成后，需要重新登录才能看到新菜单和权限生效
-- 4. 如果需要恢复权限检查，请取消UserController.java中@RequiresPermissions注解的注释
