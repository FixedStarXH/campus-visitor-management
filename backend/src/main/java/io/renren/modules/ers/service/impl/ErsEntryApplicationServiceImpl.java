package io.renren.modules.ers.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.modules.ers.dao.ErsEntryApplicationDao;
import io.renren.modules.ers.entity.ErsEntryApplicationEntity;
import io.renren.modules.ers.service.ErsEntryApplicationService;
import org.springframework.stereotype.Service;

/**
 * 入校申请表 Service 实现类
 */
@Service("ersEntryApplicationService")
public class ErsEntryApplicationServiceImpl extends ServiceImpl<ErsEntryApplicationDao, ErsEntryApplicationEntity> implements ErsEntryApplicationService {
}
