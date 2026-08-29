import request from "@/utils/request.js";

function getMonthDetail(data) {
  return request.post("/amazon/api/v1/settlement/getMonthDetail",  data  );
}

function getMonthReport(data) {
  return request.post("/amazon/api/v1/settlement/getMonthReport",  data  );
}

function quantityByDay(data) {
  return request.post("/amazon/api/v1/settlement/quantityByDay",  data  );
}

function dailyIncomeReport(data) {
  return request.post("/amazon/api/v1/settlement/dailyIncomeReport",  data  );
}

function dailyIncomeReportByTransaction(data) {
  return request.post("/amazon/api/v1/settlement/dailyIncomeReportByTransaction",  data  );
}

function getMonthReportField() {
  return request.get("/amazon/api/v1/settlement/getMonthReportField");
}

export default {
  getMonthDetail,
  getMonthReport,
  quantityByDay,
  dailyIncomeReport,
  dailyIncomeReportByTransaction,
  getMonthReportField
};