package io.renren.modules.application.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.renren.modules.application.entity.ApplicationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 入校申请DAO
 */
@Mapper
public interface ApplicationDao extends BaseMapper<ApplicationEntity> {

    /**
     * 分页查询入校申请列表
     */
    IPage<ApplicationEntity> selectApplicationPage(Page<ApplicationEntity> page,
                                                   @Param("realName") String realName,
                                                   @Param("phone") String phone,
                                                   @Param("status") Integer status,
                                                   @Param("startDate") Date startDate,
                                                   @Param("endDate") Date endDate);

    /**
     * 统计待审批申请数量
     */
    Integer countPendingApplications();

    /**
     * 批量更新申请状态
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status,
                          @Param("approverId") Long approverId, @Param("remark") String remark);

    /**
     * 批量删除申请（仅限未审批和已取消状态）
     */
    int batchDeleteByStatus(@Param("ids") List<Long> ids, @Param("allowedStatus") List<Integer> allowedStatus);
}
