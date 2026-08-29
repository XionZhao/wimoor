import request from "@/utils/request.js";

// 销售月报
function saleMonthList(data) {
    return request.post('/plantool/api/v1/plan/salemonth/list', data);
}
function saleMonthCateList(data) {
    return request.post('/plantool/api/v1/plan/salemonth/catelist', data);
}
function saleMonthSummaryList(data) {
    return request.post('/plantool/api/v1/plan/salemonth/summarylist', data);
}

// 出货计划
function shipPlanList(data) {
    return request.post('/plantool/api/v1/plan/shipplan/list', data);
}
function refreshShipPlan(data) {
    return request.post('/plantool/api/v1/plan/shipplan/refreshPlanDetail', data);
}
function selectWareHouseList(data) {
    return request.post('/plantool/api/v1/plan/shipplan/selectWareHouseList', data);
}
function selectFbaWareHouseList(data) {
    return request.post('/plantool/api/v1/plan/shipplan/selectFbaWareHouseList', data);
}
function updateDeliverycycle(data) {
    return request.post('/plantool/api/v1/plan/shipplan/updateDeliverycycle', data);
}
function updateStockcycle(data) {
    return request.post('/plantool/api/v1/plan/shipplan/updateStockcycle', data);
}
function saveAddDaySetting(data) {
    return request.post('/plantool/api/v1/plan/shipplan/saveAddDaySetting', data);
}
function saveToPlanItem(data) {
    return request.post('/plantool/api/v1/plan/shipplan/saveToPlanItem', data);
}

// 人力计划
function manPlanList(data) {
    return request.post('/plantool/api/v1/plan/manplan/list', data);
}
function refreshManPlan(data) {
    return request.post('/plantool/api/v1/plan/manplan/refreshData', data);
}
function updateProWorkHours(data) {
    return request.post('/plantool/api/v1/plan/manplan/updateProWorkHours', data);
}
function updateUnitsTime(data) {
    return request.post('/plantool/api/v1/plan/manplan/updateUnitsTime', data);
}
function loadShopWorkTime(data) {
    return request.post('/plantool/api/v1/plan/manplan/loadShopWorkTime', data);
}

// 物料需求
function purchasePlanList(data) {
    return request.post('/plantool/api/v1/plan/purchaseplan/list', data);
}
function getWeekDetail(data) {
    return request.post('/plantool/api/v1/plan/purchaseplan/getWeekDetail', data);
}
function refreshPurchasePlan(data) {
    return request.post('/plantool/api/v1/plan/purchaseplan/refreshData', data);
}
function updatePurchasedaynum(data) {
    return request.post('/plantool/api/v1/plan/purchaseplan/updatePurchasedaynum', data);
}
function checkMaterial(data) {
    return request.post('/plantool/api/v1/plan/purchaseplan/check', data);
}
function savePurchaseForm(data) {
    return request.post('/plantool/api/v1/plan/purchaseplan/saveData', data);
}
function approvePurchaseForm(data) {
    return request.post('/plantool/api/v1/plan/purchaseplan/approve', data);
}
function purchaseFormList(data) {
    return request.post('/plantool/api/v1/plan/purchaseplan/formlistData', data);
}
function purchaseFormInfo(data) {
    return request.post('/plantool/api/v1/plan/purchaseplan/formDataInfo', data);
}

// 提货付款
function pickPayList(data) {
    return request.post('/plantool/api/v1/plan/pickpay/listapply', data);
}
function loadApproveNums(data) {
    return request.post('/plantool/api/v1/plan/pickpay/loadApproveNums', data);
}
function submitPickPay(data) {
    return request.post('/plantool/api/v1/plan/pickpay/submitdata', data);
}
function approvePickPay(data) {
    return request.post('/plantool/api/v1/plan/pickpay/approvedata', data);
}

// 销售计划
function salePrePlanList(data) {
    return request.post('/plantool/api/v1/plan/salepreplan/list', data);
}
function saveSalePrePlanItem(data) {
    return request.post('/plantool/api/v1/plan/salepreplan/savePlanMonthFormEntryAndItem', data);
}
function submitSalePrePlan(data) {
    return request.post('/plantool/api/v1/plan/salepreplan/submitAudistatus', data);
}
function approveSalePrePlan(data) {
    return request.post('/plantool/api/v1/plan/salepreplan/approveAudistatus', data);
}
function getSalePrePlanWeekDetail(data) {
    return request.post('/plantool/api/v1/plan/salepreplan/getWeekDetail', data);
}

export default {
    saleMonthList, saleMonthCateList, saleMonthSummaryList,
    shipPlanList, refreshShipPlan, selectWareHouseList, selectFbaWareHouseList,
    updateDeliverycycle, updateStockcycle, saveAddDaySetting, saveToPlanItem,
    manPlanList, refreshManPlan, updateProWorkHours, updateUnitsTime, loadShopWorkTime,
    purchasePlanList, getWeekDetail, refreshPurchasePlan, updatePurchasedaynum,
    checkMaterial, savePurchaseForm, approvePurchaseForm, purchaseFormList, purchaseFormInfo,
    pickPayList, loadApproveNums, submitPickPay, approvePickPay,
    salePrePlanList, saveSalePrePlanItem, submitSalePrePlan, approveSalePrePlan, getSalePrePlanWeekDetail
}
