package io.renren.modules.ers.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.R;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.ers.dto.NoShowRuleDTO;
import io.renren.modules.ers.entity.ErsSpecialDateEntity;
import io.renren.modules.ers.entity.ErsTimeSlotEntity;
import io.renren.modules.ers.service.ErsConfigService;
import io.renren.modules.ers.service.ErsSpecialDateService;
import io.renren.modules.ers.service.ErsTimeSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ERS系统业务配置 Controller
 */
@RestController
@RequestMapping("/admin/config")
public class ErsConfigController {

    @Autowired
    private ErsConfigService ersConfigService;
    @Autowired
    private ErsTimeSlotService ersTimeSlotService;
    @Autowired
    private ErsSpecialDateService ersSpecialDateService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private R verifyToken(String token) {
        if(token == null || token.isEmpty()){
            return R.error("token不能为空");
        }
        try {
            if(stringRedisTemplate.hasKey("blacklist:" + token)){
                return R.error("token已失效，请重新登录");
            }

            io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
            if(claims == null || jwtUtils.isTokenExpired(claims.getExpiration())){
                return R.error("token失效，请重新登录");
            }
            String userType = claims.get("userType", String.class);
            if(!"ADMIN".equals(userType)){
                return R.error("无权限操作，需要管理员权限");
            }
            return null;
        } catch (Exception e) {
            return R.error("token无效，请重新登录");
        }
    }

    /**
     * 获取爽约规则
     */
    @GetMapping("/no-show")
    public R getNoShowRule(@RequestHeader("token") String token) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        NoShowRuleDTO rule = ersConfigService.getNoShowRule();
        return R.ok().put("rule", rule);
    }

    /**
     * 更新爽约规则
     */
    @SysLog("修改爽约规则")
    @PutMapping("/no-show")
    public R updateNoShowRule(@RequestHeader("token") String token, @RequestBody NoShowRuleDTO dto) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        ersConfigService.updateNoShowRule(dto);
        return R.ok();
    }

    /**
     * 获取入校时间段列表
     */
    @GetMapping("/visit-time")
    public R listTimeSlots(@RequestHeader("token") String token) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        List<ErsTimeSlotEntity> list = ersTimeSlotService.list();
        return R.ok().put("list", list);
    }

    /**
     * 新增或修改时间段
     */
    @SysLog("维护入校时间段")
    @PutMapping("/visit-time")
    public R saveOrUpdateTimeSlot(@RequestHeader("token") String token, @RequestBody ErsTimeSlotEntity timeSlot) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        ersTimeSlotService.saveOrUpdate(timeSlot);
        return R.ok();
    }

    /**
     * 添加特殊日期 (节假日/闭校日)
     */
    @SysLog("添加特殊日期配置")
    @PostMapping("/special-date/add")
    public R saveSpecialDate(@RequestHeader("token") String token, @RequestBody ErsSpecialDateEntity specialDate) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        ersSpecialDateService.save(specialDate);
        return R.ok();
    }

    /**
     * 获取特殊日期列表
     */
    @GetMapping("/special-date/list")
    public R listSpecialDates(@RequestHeader("token") String token) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        List<ErsSpecialDateEntity> list = ersSpecialDateService.list();
        return R.ok().put("list", list);
    }

    /**
     * 修改特殊日期
     */
    @SysLog("修改特殊日期配置")
    @PutMapping("/special-date/update/{id}")
    public R updateSpecialDate(@RequestHeader("token") String token, @PathVariable Long id, @RequestBody ErsSpecialDateEntity specialDate) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        ErsSpecialDateEntity exist = ersSpecialDateService.getById(id);
        if(exist == null){
            return R.error("该特殊日期不存在");
        }

        specialDate.setSpecialDateId(id);
        ersSpecialDateService.updateById(specialDate);
        return R.ok();
    }

    /**
     * 删除特殊日期
     */
    @SysLog("删除特殊日期配置")
    @DeleteMapping("/special-date/delete/{id}")
    public R deleteSpecialDate(@RequestHeader("token") String token, @PathVariable Long id) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        ErsSpecialDateEntity exist = ersSpecialDateService.getById(id);
        if(exist == null){
            return R.error("该特殊日期不存在");
        }

        ersSpecialDateService.removeById(id);
        return R.ok();
    }
}
