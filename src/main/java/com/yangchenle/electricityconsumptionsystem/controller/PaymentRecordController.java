package com.yangchenle.electricityconsumptionsystem.controller;

import com.yangchenle.electricityconsumptionsystem.common.CommonResult;
import com.yangchenle.electricityconsumptionsystem.constant.ElectricState;
import com.yangchenle.electricityconsumptionsystem.constant.PaymentState;
import com.yangchenle.electricityconsumptionsystem.constant.SessionParameters;
import com.yangchenle.electricityconsumptionsystem.dto.ElectricDTO;
import com.yangchenle.electricityconsumptionsystem.dto.PaymentRecordDTO;
import com.yangchenle.electricityconsumptionsystem.dto.TypeTableDTO;
import com.yangchenle.electricityconsumptionsystem.dto.UserDTO;
import com.yangchenle.electricityconsumptionsystem.emun.HttpStatus;
import com.yangchenle.electricityconsumptionsystem.service.ElectricService;
import com.yangchenle.electricityconsumptionsystem.service.PaymentRecordService;
import com.yangchenle.electricityconsumptionsystem.service.UserService;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import com.yangchenle.electricityconsumptionsystem.service.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class PaymentRecordController {

    @Resource
    private PaymentRecordService paymentRecordService;

    @Resource
    private ElectricService electricService;

    @Resource
    private UserService userService;

    /**
     * 用户查看自己未缴费记录
     *
     * @return
     */
    @GetMapping("/user/query/payment")
    public CommonResult queryPaymentRecord(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute(SessionParameters.USERID);
        List<ElectricDTO> electricDTOList = electricService.queryEleByUserId(userId);
        if (CollectionUtils.isEmpty(electricDTOList)) {
            return CommonResult.fail(404, "没有该用户电表记录");
        }
        Map<String, List<PaymentRecordDTO>> paymentMap = new HashMap<>();
        for (ElectricDTO electric : electricDTOList) {
            List<PaymentRecordDTO> paymentRecordDTOS = paymentRecordService.queryPayment(electric.getElectricId(), PaymentState.UNPAID);
            if (CollectionUtils.isEmpty(paymentRecordDTOS)) {
                continue;
            }
            paymentMap.put("paymentInfo", paymentRecordDTOS);
        }
        return CommonResult.success(paymentMap);
    }
    @Resource
    private TypeTableService typeTableService;

    @Resource
    private DeductionService deductionService;

//    /**
//     * 用户查看自己未缴费记录
//     *
//     * @return
//     */
////    @GetMapping("/user/query/payment")
//    public CommonResult queryPaymentRecord(HttpServletRequest request){
//        HttpSession session = request.getSession();
//        Integer userId = (Integer) session.getAttribute(SessionParameters.USERID);
//        List<ElectricDTO> electricDTOList = electricService.queryEleByUserId(userId);
//        if (CollectionUtils.isEmpty(electricDTOList)){
//            return CommonResult.fail(404,"没有该用户电表记录");
//        }
//        Map<String,List<PaymentRecordDTO>> paymentMap = new HashMap<>();
//        for (ElectricDTO electric: electricDTOList) {
//            List<PaymentRecordDTO> paymentRecordDTOS = paymentRecordService.queryPayment(electric.getElectricId(), PaymentState.UNPAID);
//            if (CollectionUtils.isEmpty(paymentRecordDTOS)){
//                continue;
//            }
//            paymentMap.put("paymentInfo",paymentRecordDTOS);
//        }
//        return CommonResult.success(paymentMap);
//    }

    /**
     * 缴费
     *
     * @param paymentMethod
     * @param money
     * @param electricId
     * @return
     */
    @GetMapping("/user/payMoney")
    public CommonResult payMoney(Integer paymentMethod, BigDecimal money,
                                 Integer electricId, HttpServletRequest request){
        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute(SessionParameters.USERID);
        if (paymentMethod == null || electricId == null){
            return CommonResult.fail(403,"参数错误！");
        }
        UserDTO userDTO = userService.queryById(userId);
        BigDecimal price = userDTO.getPrice();
        if (price.compareTo(money) < 0 ){
            return CommonResult.fail(500,"余额不足！");
        }
        int userPayResult = userService.payById(price.subtract(money),userId);
        if (userPayResult <= 0){
            return CommonResult.fail(500,"支付失败！");
        }
        UserDTO userDTO1 = userService.queryById(1);
        BigDecimal bossMoney = userDTO1.getPrice();
        BigDecimal bossMoneyNum = bossMoney.add(money);
        int payResult = userService.payById(bossMoneyNum, 1);
        if (payResult <= 0){
            return CommonResult.fail(500,"转账失败！");
        }
        int result = paymentRecordService.updatePayment(paymentMethod,new BigDecimal(0),PaymentState.PAID,electricId);
        if (result <= 0){
            return CommonResult.fail(500,"缴费失败！");
        }
        ElectricDTO electricDTO = electricService.selectElectricById(electricId);
        BigDecimal elePrice = electricDTO.getMoney();
        BigDecimal priceNum = elePrice.add(money);
        int r = priceNum.compareTo(BigDecimal.ZERO);
        if (r < 0){
            int payResults = electricService.updateElectric(null,priceNum, ElectricState.STOP,electricId);
            if (payResults <= 0){
                return CommonResult.fail(500,"更改电表信息失败！");
            }
            return CommonResult.success();
        }
        int payResulted = electricService.updateElectric(null,priceNum,ElectricState.NORMAL,electricId);
        if (payResulted <= 0){
            return CommonResult.fail(500,"更改电表信息失败！");
        }
        Integer type = electricDTO.getType();
        TypeTableDTO typeTableDTO = typeTableService.selectById(type);
        BigDecimal typePrice = typeTableDTO.getPrice();
        BigDecimal consumption = money.subtract(typePrice);
//        int data = deductionService.
        return CommonResult.success();
    }

    @GetMapping("/select/payRecord")
    public CommonResult selectPayRecord(@RequestParam(required = false) Integer electricNum,
                                        @RequestParam(required = false) String start,
                                        @RequestParam(required = false) String end) {
        Integer electricId = null;
        Date startTime = null;
        Date endTime = null;
        if (electricNum != null) {
            ElectricDTO electricDTO = electricService.selectElectricByNum(electricNum);
            if (electricDTO != null) {
                electricId = electricDTO.getElectricId();
            } else {
                return CommonResult.fail(403, "电表编号不存在");
            }
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if (!StringUtils.isEmpty(start)) {
                startTime = sdf.parse(start);
            }
            if (!StringUtils.isEmpty(end)) {
                endTime = sdf.parse(end);
                endTime = new DateTime(endTime).plusDays(1).toDate();
            }
            List<PaymentRecordDTO> recordDTOS = paymentRecordService.selectPayRecordAll(electricId, startTime, endTime);
            if (CollectionUtils.isEmpty(recordDTOS)) {
                return CommonResult.fail(HttpStatus.NOT_FOUND);
            }
            return CommonResult.success(createRePayRecord(recordDTOS));
        } catch (ParseException e) {
            System.out.println("时间格式错误");
            return CommonResult.fail(HttpStatus.PARAMETER_ERROR);
        }
    }

    private List<rePayRecord> createRePayRecord(List<PaymentRecordDTO> list) {
        List<rePayRecord> rePayRecords = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
            return rePayRecords;
        }
        for (PaymentRecordDTO pay : list) {
            rePayRecord rePayRecord = new rePayRecord();
            BeanUtils.copyProperties(pay, rePayRecord);
            ElectricDTO electricDTO = electricService.selectElectricById(pay.getElectricId());
            if (electricDTO != null) {
                rePayRecord.setElectricNum(electricDTO.getNum());
                if (electricDTO.getUserId() != null) {
                    UserDTO userDTO = userService.queryById(electricDTO.getUserId());
                    if (userDTO != null) {
                        rePayRecord.setUsername(userDTO.getUserName());
                    } else {
                        rePayRecord.setUsername("信息有误");
                    }
                } else {
                    rePayRecord.setUsername("暂未绑定");
                }
                rePayRecords.add(rePayRecord);
            }
        }
        return rePayRecords;
    }

    @Data
    private class rePayRecord {
        private Integer paymentId;
        private Integer electricId;
        private Integer electricNum;
        private String username;
        private Integer paymentMethod;
        private Integer paymentState;
        private BigDecimal money;
        private Date addTime;
    }
}
