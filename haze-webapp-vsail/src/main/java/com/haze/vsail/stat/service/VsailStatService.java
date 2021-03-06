package com.haze.vsail.stat.service;

import com.haze.common.util.HazeDateUtils;
import com.haze.common.util.HazeJsonUtils;
import com.haze.common.util.HazeStringUtils;
import com.haze.core.jpa.repository.HazeSpecification;
import com.haze.system.entity.Group;
import com.haze.system.service.GroupService;
import com.haze.system.utils.Status;
import com.haze.vsail.stat.dao.BusBreakDownLogDao;
import com.haze.vsail.stat.dao.BusFireLogDao;
import com.haze.vsail.stat.dao.BusOnOffLogDao;
import com.haze.vsail.stat.entity.BusBreakDownLog;
import com.haze.vsail.stat.entity.BusFireLog;
import com.haze.vsail.stat.entity.BusOnOffLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.Query;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class VsailStatService {

    private BusOnOffLogDao busOnOffLogDao;

    private BusFireLogDao busFireLogDao;

    private BusBreakDownLogDao busBreakDownLogDao;

    private GroupService groupService;

    private EntityManager em;

    public VsailStatService(BusOnOffLogDao busOnOffLogDao, BusFireLogDao busFireLogDao, BusBreakDownLogDao busBreakDownLogDao, EntityManager em, GroupService groupService) {
        this.busOnOffLogDao = busOnOffLogDao;
        this.busFireLogDao = busFireLogDao;
        this.busBreakDownLogDao = busBreakDownLogDao;
        this.em = em;
        this.groupService = groupService;
    }

    /**
     * 获取车辆上线下线分页列表
     * @param pageable 分页信息
     * @param queryParams 查询参数
     * @return 车辆上线下线分页数据
     */
    public Page<BusOnOffLog> findOnOffPage(Pageable pageable, Map<String, Object> queryParams) {
        Specification<BusOnOffLog> spec = new HazeSpecification<>(queryParams);
        return this.busOnOffLogDao.findAll(spec, pageable);
    }

    /**
     * 获取车辆火警分页列表
     * @param pageable 分页信息
     * @param queryParams 查询参数
     * @return 车辆上线下线分页数据
     */
    public Page<BusFireLog> findFirePage(Pageable pageable, Map<String, Object> queryParams) {
        Specification<BusFireLog> spec = new HazeSpecification<>(queryParams);
        return this.busFireLogDao.findAll(spec, pageable);
    }

    /**
     * 获取车辆上线下线分页列表
     * @param pageable 分页信息
     * @param queryParams 查询参数
     * @return 车辆上线下线分页数据
     */
    public Page<BusBreakDownLog> findBreakDownPage(Pageable pageable, Map<String, Object> queryParams) {
        Specification<BusBreakDownLog> spec = new HazeSpecification<>(queryParams);
        return this.busBreakDownLogDao.findAll(spec, pageable);
    }
    //------------------------------------------------------------------TODO 下面统计查询如果频率高可将查询结果采用redis + 设置过期失效来处理---------------------------------------------------------//
    /**
     * 根据公交公司ID,开始日期，结束日期获取不同车型在该段日期内每一天火警数量
     * @param rootGroupId 公交公司ID
     * @param startDay 开始日期如果为null则默认"1970-01-01"
     * @param endDay 结束日期 如果为null则默认当天日期
     * @return [车型,年份，月份，天，火警数量]列表
     */
    public List<Object[]> getFireCountByBusModel(Long rootGroupId, Date startDay, Date endDay) {
        StringBuilder sql = new StringBuilder("select model_name, log_year, log_month, log_day,count(1) ct from v_bus_fire_log f where log_year>=:startYear and log_year<=:endYear and log_month>=:startMonth and log_month<=:endMonth and log_day>=:startDay and log_day<=:endDay");
        return queryBusStat(sql, "model_name", rootGroupId, processDate(startDay, endDay));
    }

    /**
     * 根据公交公司ID,开始日期，结束日期获取不同车型在该段日期内每一天故障数量
     * @param rootGroupId 公交公司ID
     * @param startDay 开始日期如果为null则默认"1970-01-01"
     * @param endDay 结束日期 如果为null则默认当天日期
     * @return [车型,年份，月份，天，故障数量]列表
     */
    public List<Object[]> getBreakDownCountByBusModel(Long rootGroupId, Date startDay, Date endDay) {
        StringBuilder sql = new StringBuilder("select model_name, log_year, log_month, log_day ,count(1) ct from v_bus_break_down_log f where log_year>=:startYear and log_year<=:endYear and log_month>=:startMonth and log_month<=:endMonth and log_day>=:startDay and log_day<=:endDay");
        return queryBusStat(sql, "model_name", rootGroupId, processDate(startDay, endDay));
    }

    /**
     * 根据公交公司ID,开始日期，结束日期获取不同线路在该段日期内每一天火警数量
     * @param rootGroupId 公交公司ID
     * @param startDay 开始日期如果为null则默认"1970-01-01"
     * @param endDay 结束日期 如果为null则默认当天日期
     * @return [线路,年份，月份，天，火警数量]列表
     */
    public List<Object[]> getFireCountByGroup(Long rootGroupId, Date startDay, Date endDay) {
        StringBuilder sql = new StringBuilder("select group_name, log_year, log_month, log_day,count(1) ct from v_bus_fire_log f where log_year>=:startYear and log_year<=:endYear and log_month>=:startMonth and log_month<=:endMonth and log_day>=:startDay and log_day<=:endDay");
        return queryBusStat(sql, "group_name", rootGroupId, processDate(startDay, endDay));
    }

    /**
     * 根据公交公司ID,开始日期，结束日期获取不同线路在该段日期内每一天故障数量
     * @param rootGroupId 公交公司ID
     * @param startDay 开始日期如果为null则默认"1970-01-01"
     * @param endDay 结束日期 如果为null则默认当天日期
     * @return [线路,年份，月份，天，故障数量]列表
     */
    public List<Object[]> getBreakDownCountByGroup(Long rootGroupId, Date startDay, Date endDay) {
        StringBuilder sql = new StringBuilder("select group_name, log_year, log_month, log_day ,count(1) ct from v_bus_break_down_log f where log_year>=:startYear and log_year<=:endYear and log_month>=:startMonth and log_month<=:endMonth and log_day>=:startDay and log_day<=:endDay");
        return queryBusStat(sql, "group_name", rootGroupId, processDate(startDay, endDay));
    }

    public List<Object[]> getFireCount(Long rootGroupId, Date startDay, Date endDay) {
        StringBuilder sql = new StringBuilder("select log_year, log_month, log_day,count(1) ct from v_bus_fire_log f where log_year>=:startYear and log_year<=:endYear and log_month>=:startMonth and log_month<=:endMonth and log_day>=:startDay and log_day<=:endDay");
        return queryBusStat(sql, null, rootGroupId, processDate(startDay, endDay));
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> queryBusStat(StringBuilder sql, String groupBy, Long rootGroupId, Map<String, Object> queryParams) {
        /*if (rootGroupId != null) {
            sql.append(" and root_Group_Id=:rootGroupId");
            queryParams.put("rootGroupId", rootGroupId);
        }*/
        //获取用户所属机构
        if (rootGroupId != null) {
            Group group = this.groupService.findById(rootGroupId);
            List<Long> groupIds = new ArrayList<>();
            groupIds.add(rootGroupId);
            for (Group g : group.getChildList(Status.ENABLE)) {
                groupIds.add(g.getId());
            }
            sql.append(" and group_Id in (:groupIds)");
            queryParams.put("groupIds", groupIds);
        }
        sql.append(" group by ");
        if (HazeStringUtils.isNotBlank(groupBy)) {
            sql.append(groupBy);
            sql.append(",");
        }
        sql.append("log_year, log_month, log_day");
        Query query = em.createNativeQuery(sql.toString());
        setParams(queryParams, query);
        return query.getResultList();
    }

    /**
     * 处理开始日期和结束日期
     * @param startDay 开始日期
     * @param endDay 结束日期
     * @return 包含年 月 日 查询参数
     */
    private Map<String, Object> processDate(Date startDay, Date endDay) {
        LocalDate startDate = LocalDate.ofYearDay(1970, 1);
        LocalDate endDate = LocalDate.now();
        if (startDay != null) {
            startDate = HazeDateUtils.toLocalDate(startDay);
        }
        if (endDay != null) {
            endDate = HazeDateUtils.toLocalDate(endDay);
        }

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("startYear", startDate.getYear());
        queryParams.put("startMonth", startDate.getMonthValue());
        queryParams.put("startDay", startDate.getDayOfMonth());
        queryParams.put("endYear", endDate.getYear());
        queryParams.put("endMonth", endDate.getMonthValue());
        queryParams.put("endDay", endDate.getDayOfMonth());
        return queryParams;
    }

    /**
     * 设置查询参数
     * @param queryParams 查询参数
     * @param query 查询对象
     */
    private void setParams(Map<String, Object> queryParams, Query query) {
        for (Parameter param : query.getParameters()) {
            String name = param.getName();
            if (StringUtils.hasText(name)) {
                query.setParameter(name, queryParams.get(name));
            } else {
                throw new IllegalArgumentException("Query查询语句命名参数不能为空");
            }
        }
    }

    public List<BusOnOffLog> queryOnOffLog(Date startDay, Date endDay) {
        return this.busOnOffLogDao.findAll(new HazeSpecification<>(processParams(startDay, endDay)));
    }

    public List<BusFireLog> queryFireLog(Date startDay, Date endDay) {
        return this.busFireLogDao.findAll(new HazeSpecification<>(processParams(startDay, endDay)));
    }

    public List<BusBreakDownLog> queryBreakDownLog(Date startDay, Date endDay) {
        return this.busBreakDownLogDao.findAll(new HazeSpecification<>(processParams(startDay, endDay)));
    }

    private Map<String, Object> processParams(Date startDay, Date endDay) {
        LocalDate startDate = LocalDate.ofYearDay(1970, 1);
        LocalDate endDate = LocalDate.now();
        if (startDay != null) {
            startDate = HazeDateUtils.toLocalDate(startDay);
        }
        if (endDay != null) {
            endDate = HazeDateUtils.toLocalDate(endDay);
        }
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("year_between", new Object[]{startDate.getYear(), endDate.getYear()});
        queryParams.put("month_between", new Object[]{startDate.getMonthValue(), endDate.getMonthValue()});
        queryParams.put("day_between", new Object[]{startDate.getDayOfMonth(), endDate.getDayOfMonth()});
        return queryParams;
    }


    /**
     * 根据车辆vin码获取最后10条传感器数据
     * @param vin 车辆vin码
     * @return [vin, 数据发送时间, 传感器数量, 是否火警, 是否故障, 传感器编号,传感器温度,传感器一氧化碳浓度, 传感器报文[1-6]]
     */
    public List<Object[]> getSensor(String vin) {
        String sql = "select * from (select vin, upload_time, array_length(sensores,1) ct, bus_data->>'isFire' as isFire,bus_data->>'isError' as isError";
        for (int i = 1; i <=6; i++) {
            sql += ",sensores["+i+"]->>'sn' as sn"+i+", sensores["+i+"]->>'temp' as temp"+i+", sensores["+i+"]->>'concen' as concen"+i+", sensores["+i+"]->>'fire' as fire"+i+", sensores["+i+"]->>'error' as error"+i +", sensores["+i+"]->>'state' as state"+i;
        }
        sql += " from v_bus_data_log where array_length(sensores,1) is not null  and vin=:vin order by upload_time desc limit 10)b order by upload_time asc";
        Query query = em.createNativeQuery(sql);
        query.setParameter("vin", vin);
        return query.getResultList();
    }


    public List<Object[]> getTestData() {
        String sql = " select upload_time, data from v_bus_data_log order by upload_time desc limit 500";
        Query query = em.createNativeQuery(sql);
        return query.getResultList();
    }
}
