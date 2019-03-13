package com.yangchenle.electricityconsumptionsystem.impl;

import com.yangchenle.electricityconsumptionsystem.constant.ElectricState;
import com.yangchenle.electricityconsumptionsystem.constant.ElectricType;
import com.yangchenle.electricityconsumptionsystem.dao.ElectricDao;
import com.yangchenle.electricityconsumptionsystem.dto.ElectricDTO;
import com.yangchenle.electricityconsumptionsystem.entity.ElectricEntity;
import com.yangchenle.electricityconsumptionsystem.service.ElectricService;
import com.yangchenle.electricityconsumptionsystem.util.BeansListUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ElectricServiceImpl implements ElectricService {

    @Resource
    private ElectricDao electricDao;

    @Override
    public List<ElectricDTO> queryEleByUserId(Integer userId) {
        if (userId == null) {
            return null;
        }
        List<ElectricEntity> electricEntityList = electricDao.queryEleByUserId(userId);
        return BeansListUtils.copyListProperties(electricEntityList, ElectricDTO.class);
    }

    @Override
    public int addElectric(Integer num, Integer type, BigDecimal data) {
        if (num == null || num < 1 || type == null ||
                type < ElectricType.HOEM || type > ElectricType.FACTORY ||
                data == null || data.compareTo(BigDecimal.valueOf(0)) < 0) {
            return 0;
        }
        ElectricEntity electricEntity = new ElectricEntity();
        electricEntity.setLastData(data);
        electricEntity.setMoney(BigDecimal.valueOf(0));
        electricEntity.setState(ElectricState.NORMAL);
        electricEntity.setType(type);
        electricEntity.setNum(num);
        return electricDao.addElectric(electricEntity);
    }

    @Override
    public int updateElectric(BigDecimal lastData, BigDecimal money, Integer state, Integer id) {
        if (id == null || id < 1) {
            return 0;
        }
        return electricDao.updateElectric(lastData, money, state, id);
    }

    @Override
    public int deleteElectricById(Integer id) {
        if (id == null || id < 1) {
            return 0;
        }
        return electricDao.deleteElectricById(id);
    }

    @Override
    public ElectricDTO selectElectricById(Integer id) {
        if (id == null || id < 1) {
            return null;
        }
        ElectricEntity electricEntity = electricDao.selectElectricById(id);
        if (electricEntity == null) {
            return null;
        }
        ElectricDTO electricDTO = new ElectricDTO();
        BeanUtils.copyProperties(electricEntity, electricDTO);
        return electricDTO;
    }

    @Override
    public List<ElectricDTO> selectElectricAll() {
        List<ElectricEntity> electricEntities = electricDao.selectElectricAll();
        if (CollectionUtils.isEmpty(electricEntities)) {
            return new ArrayList<>();
        }
        return BeansListUtils.copyListProperties(electricEntities, ElectricDTO.class);
    }

    @Override
    public ElectricDTO selectElectricByNum(Integer num) {
        if (num == null || num < 0) {
            return null;
        }
        ElectricEntity electricEntity = electricDao.selectElectricByNum(num);
        if (electricEntity == null) {
            return null;
        }
        ElectricDTO electricDTO = new ElectricDTO();
        BeanUtils.copyProperties(electricEntity, electricDTO);
        return electricDTO;
    }
}
