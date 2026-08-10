package io.renren.modules.ers.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.modules.ers.dao.ErsSpecialDateDao;
import io.renren.modules.ers.entity.ErsSpecialDateEntity;
import io.renren.modules.ers.service.ErsSpecialDateService;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 特殊日期配置表 Service 实现类
 */
@Service("ersSpecialDateService")
public class ErsSpecialDateServiceImpl extends ServiceImpl<ErsSpecialDateDao, ErsSpecialDateEntity> implements ErsSpecialDateService {

    @Override
    public boolean checkDateAvailable(String date) {
        ErsSpecialDateEntity specialDate = this.getOne(new QueryWrapper<ErsSpecialDateEntity>()
                .eq("special_date", date)
                .eq("status", 1));
        
        if (specialDate != null) {
            // dateType: 1是节假日(开放) 2是闭校日(禁止)
            return specialDate.getDateType() == 1;
        }
        
        // 如果没有特殊配置，默认返回 true (可按常规时段申请)
        return true;
    }
}
